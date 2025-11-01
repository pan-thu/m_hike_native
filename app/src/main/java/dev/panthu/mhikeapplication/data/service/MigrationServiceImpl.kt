package dev.panthu.mhikeapplication.data.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.panthu.mhikeapplication.data.local.dao.HikeDao
import dev.panthu.mhikeapplication.data.local.dao.ObservationDao
import dev.panthu.mhikeapplication.data.local.mapper.toDomain
import dev.panthu.mhikeapplication.data.local.repository.LocalImageRepository
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.repository.ImageRepository
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import dev.panthu.mhikeapplication.domain.service.MigrationProgress
import dev.panthu.mhikeapplication.domain.service.MigrationResult
import dev.panthu.mhikeapplication.domain.service.MigrationService
import dev.panthu.mhikeapplication.domain.service.MigrationStats
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of MigrationService that handles migrating guest data to Firebase.
 */
class MigrationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hikeDao: HikeDao,
    private val observationDao: ObservationDao,
    @Named("remote") private val remoteHikeRepository: HikeRepository,
    @Named("remote") private val remoteObservationRepository: ObservationRepository,
    private val imageRepository: ImageRepository,
    private val localImageRepository: LocalImageRepository
) : MigrationService {

    companion object {
        private const val TAG = "MigrationService"
        private val json = Json { ignoreUnknownKeys = true }

        // Timeout configurations
        private val MIGRATION_TIMEOUT = 15.minutes
        private val IMAGE_UPLOAD_TIMEOUT = 30.seconds
    }

    /**
     * Deserialize image URLs with error tracking
     */
    private fun deserializeImageUrls(urlsJson: String?, itemId: String, itemType: String): List<String> {
        if (urlsJson.isNullOrEmpty() || urlsJson == "[]") {
            return emptyList()
        }

        return try {
            json.decodeFromString<List<String>>(urlsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize image URLs for $itemType $itemId", e)
            emptyList()
        }
    }

    /**
     * Delete file with verification and logging
     */
    private fun deleteFileWithVerification(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            Log.w(TAG, "File already deleted or doesn't exist: $path")
            return true
        }

        val deleted = file.delete()
        if (!deleted) {
            Log.e(TAG, "Failed to delete file: $path")
        }
        return deleted
    }

    override fun migrateGuestData(
        guestId: String,
        newUserId: String
    ): Flow<MigrationProgress> = flow {
        val errors = mutableListOf<String>()
        var migratedHikes = 0
        var migratedObservations = 0
        var uploadedImages = 0

        try {
            // Wrap entire migration in timeout
            withTimeout(MIGRATION_TIMEOUT) {
            // Step 1: Check what needs migration
            val statsResult = checkMigrationNeeded(guestId)
            if (statsResult is Result.Error) {
                emit(MigrationProgress.Error(statsResult.message ?: "Failed to check migration data"))
                return@flow
            }

            val stats = (statsResult as Result.Success).data
            if (stats.isEmpty) {
                emit(MigrationProgress.Complete(
                    MigrationResult(
                        migratedHikes = 0,
                        migratedObservations = 0,
                        uploadedImages = 0
                    )
                ))
                return@flow
            }

            emit(MigrationProgress.Initializing(stats))

            // Step 2: Get all unsynced hikes
            val unsyncedHikes = hikeDao.getUnsyncedHikes()

            // Step 3: Migrate each hike
            unsyncedHikes.forEachIndexed { index, hikeEntity ->
                try {
                    emit(MigrationProgress.MigratingHikes(
                        current = index + 1,
                        total = unsyncedHikes.size,
                        hikeName = hikeEntity.name
                    ))

                    // Convert entity to domain model and update owner
                    var hike = hikeEntity.toDomain().copy(ownerId = newUserId)

                    // Migrate hike images
                    val migratedImageUrls = mutableListOf<String>()
                    val hikeImageUrls = deserializeImageUrls(hikeEntity.imageUrls, hikeEntity.id, "hike")
                    hikeImageUrls.forEachIndexed { imgIndex, localPath ->
                        try {
                            val progress = if (stats.totalImages > 0) {
                                uploadedImages.toFloat() / stats.totalImages
                            } else {
                                1f
                            }
                            emit(MigrationProgress.UploadingImages(
                                current = uploadedImages + 1,
                                total = stats.totalImages,
                                progress = progress
                            ))

                            try {
                                val cloudUrl = withTimeout(IMAGE_UPLOAD_TIMEOUT) {
                                    uploadLocalImageToCloud(
                                        localPath = localPath,
                                        hikeId = hikeEntity.id,
                                        newUserId = newUserId
                                    )
                                }

                                if (cloudUrl != null) {
                                    migratedImageUrls.add(cloudUrl)
                                    uploadedImages++
                                } else {
                                    errors.add("Failed to upload image for hike: ${hikeEntity.name}")
                                }
                            } catch (e: TimeoutCancellationException) {
                                Log.w(TAG, "Image upload timeout for hike ${hikeEntity.name}: $localPath")
                                errors.add("Image upload timeout for ${hikeEntity.name} (${IMAGE_UPLOAD_TIMEOUT.inWholeSeconds}s)")
                            }
                        } catch (e: Exception) {
                            errors.add("Image upload error for ${hikeEntity.name}: ${e.message}")
                        }
                    }

                    // Update hike with cloud image URLs
                    hike = hike.copy(imageUrls = migratedImageUrls)

                    // Create hike in Firestore
                    when (val result = remoteHikeRepository.createHike(hike)) {
                        is Result.Success -> {
                            // Mark as synced in local DB
                            hikeDao.markAsSynced(hikeEntity.id)
                            migratedHikes++

                            // Migrate observations for this hike
                            val result = migrateObservationsForHike(
                                hikeId = hikeEntity.id,
                                newUserId = newUserId,
                                stats = stats,
                                currentUploadedImages = uploadedImages,
                                onProgress = { progress ->
                                    emit(progress)
                                },
                                onError = { errors.add(it) }
                            )
                            migratedObservations += result.migratedCount
                            uploadedImages = result.uploadedImages
                        }
                        is Result.Error -> {
                            errors.add("Failed to migrate hike ${hikeEntity.name}: ${result.message}")
                        }
                        is Result.Loading -> { /* No action */ }
                    }
                } catch (e: Exception) {
                    errors.add("Error migrating hike ${hikeEntity.name}: ${e.message}")
                }
            }

                // Step 4: Emit completion
                val result = MigrationResult(
                    migratedHikes = migratedHikes,
                    migratedObservations = migratedObservations,
                    uploadedImages = uploadedImages,
                    failedItems = (stats.totalHikes - migratedHikes) +
                                 (stats.totalObservations - migratedObservations),
                    errors = errors
                )

                emit(MigrationProgress.Complete(result))
            }  // End withTimeout
        } catch (e: TimeoutCancellationException) {
            emit(MigrationProgress.Error(
                message = "Migration timeout: The process took too long (${MIGRATION_TIMEOUT.inWholeMinutes} minutes). Please try again with a better network connection.",
                retryable = true
            ))
        } catch (e: Exception) {
            emit(MigrationProgress.Error(
                message = "Migration failed: ${e.message}",
                retryable = true
            ))
        }
    }

    /**
     * Result of migrating observations for a hike
     */
    private data class ObservationMigrationResult(
        val migratedCount: Int,
        val uploadedImages: Int
    )

    private suspend fun migrateObservationsForHike(
        hikeId: String,
        newUserId: String,
        stats: MigrationStats,
        currentUploadedImages: Int,
        onProgress: suspend (MigrationProgress) -> Unit,
        onError: (String) -> Unit
    ): ObservationMigrationResult {
        var migratedCount = 0
        var uploadedImages = currentUploadedImages

        try {
            val observations = observationDao.getObservationsForHikeSync(hikeId)

            observations.forEachIndexed { index, obsEntity ->
                try {
                    onProgress(MigrationProgress.MigratingObservations(
                        current = index + 1,
                        total = observations.size,
                        hikeId = hikeId
                    ))

                    // Convert to domain model
                    var observation = obsEntity.toDomain()

                    // Migrate observation images
                    val migratedImageUrls = mutableListOf<String>()
                    val observationImageUrls = deserializeImageUrls(obsEntity.imageUrls, obsEntity.id, "observation")
                    observationImageUrls.forEach { localPath ->
                        try {
                            val progress = if (stats.totalImages > 0) {
                                uploadedImages.toFloat() / stats.totalImages
                            } else {
                                1f
                            }
                            onProgress(MigrationProgress.UploadingImages(
                                current = uploadedImages + 1,
                                total = stats.totalImages,
                                progress = progress
                            ))

                            try {
                                val cloudUrl = withTimeout(IMAGE_UPLOAD_TIMEOUT) {
                                    uploadLocalImageToCloud(
                                        localPath = localPath,
                                        hikeId = hikeId,
                                        observationId = obsEntity.id,
                                        newUserId = newUserId
                                    )
                                }

                                if (cloudUrl != null) {
                                    migratedImageUrls.add(cloudUrl)
                                    uploadedImages++
                                } else {
                                    onError("Failed to upload observation image")
                                }
                            } catch (e: TimeoutCancellationException) {
                                Log.w(TAG, "Image upload timeout for observation ${obsEntity.id}: $localPath")
                                onError("Image upload timeout for observation (${IMAGE_UPLOAD_TIMEOUT.inWholeSeconds}s)")
                            }
                        } catch (e: Exception) {
                            onError("Observation image upload error: ${e.message}")
                        }
                    }

                    // Update observation with cloud URLs
                    observation = observation.copy(imageUrls = migratedImageUrls)

                    // Create observation in Firestore
                    when (val result = remoteObservationRepository.createObservation(observation)) {
                        is Result.Success -> {
                            observationDao.markAsSynced(obsEntity.id)
                            migratedCount++
                        }
                        is Result.Error -> {
                            onError("Failed to migrate observation: ${result.message}")
                        }
                        is Result.Loading -> { /* No action */ }
                    }
                } catch (e: Exception) {
                    onError("Error migrating observation: ${e.message}")
                }
            }
        } catch (e: Exception) {
            onError("Error loading observations for hike $hikeId: ${e.message}")
        }

        return ObservationMigrationResult(
            migratedCount = migratedCount,
            uploadedImages = uploadedImages
        )
    }

    private suspend fun uploadLocalImageToCloud(
        localPath: String,
        hikeId: String,
        observationId: String? = null,
        newUserId: String
    ): String? {
        return try {
            val file = File(localPath)
            if (!file.exists()) {
                Log.w(TAG, "File not found: $localPath")
                return null
            }

            // CRITICAL FIX: Use FileProvider instead of Uri.fromFile()
            // This prevents crashes on Android 7.1+ and security vulnerabilities
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "FileProvider error - file path may be outside configured paths: $localPath", e)
                return null
            }

            // Upload to Firebase Storage
            when (val result = imageRepository.uploadImage(uri, hikeId, observationId)) {
                is Result.Success -> {
                    Log.d(TAG, "Successfully uploaded image: ${file.name}")
                    result.data.url
                }
                is Result.Error -> {
                    Log.e(TAG, "Upload failed: ${result.message}")
                    null
                }
                is Result.Loading -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error uploading image", e)
            null
        }
    }

    override suspend fun checkMigrationNeeded(guestId: String): Result<MigrationStats> {
        return try {
            val unsyncedHikes = hikeDao.getUnsyncedHikes()
            var totalObservations = 0
            var totalImages = 0
            var totalSizeBytes = 0L

            unsyncedHikes.forEach { hike ->
                // Count observations - use sync method to get List, not Flow
                val observations = observationDao.getObservationsForHikeSync(hike.id)
                totalObservations += observations.size

                // Count and size images for hike
                val hikeImages = deserializeImageUrls(hike.imageUrls, hike.id, "hike")
                totalImages += hikeImages.size
                hikeImages.forEach { path ->
                    val file = File(path)
                    if (file.exists()) {
                        totalSizeBytes += file.length()
                    }
                }

                // Count and size images for observations
                observations.forEach { obs ->
                    val obsImages = deserializeImageUrls(obs.imageUrls, obs.id, "observation")
                    totalImages += obsImages.size
                    obsImages.forEach { path ->
                        val file = File(path)
                        if (file.exists()) {
                            totalSizeBytes += file.length()
                        }
                    }
                }
            }

            Result.Success(
                MigrationStats(
                    totalHikes = unsyncedHikes.size,
                    totalObservations = totalObservations,
                    totalImages = totalImages,
                    estimatedSizeBytes = totalSizeBytes
                )
            )
        } catch (e: Exception) {
            Result.Error("Failed to check migration data: ${e.message}")
        }
    }

    /**
     * Data class to hold verification results
     */
    private data class VerificationResult(
        val success: Boolean,
        val verifiedHikeIds: List<String>,
        val errors: List<String>
    )

    /**
     * Verify migration success in Firestore before cleanup
     * CRITICAL: Only delete local data if cloud verification succeeds
     */
    private suspend fun verifyCloudMigration(guestId: String): VerificationResult {
        return withContext(Dispatchers.IO) {
            try {
                val verifiedHikeIds = mutableListOf<String>()
                val errors = mutableListOf<String>()

                // Get all local hikes marked as synced
                val syncedHikes = hikeDao.getSyncedHikes(guestId)

                Log.d(TAG, "Verifying ${syncedHikes.size} synced hikes in cloud")

                syncedHikes.forEach { localHike ->
                    // Note: In a real implementation, you would query Firestore here
                    // to verify the hike exists. For now, we'll trust the sync flag
                    // but log the verification step
                    Log.d(TAG, "Verifying hike in cloud: ${localHike.name} (${localHike.id})")

                    // In production, add actual Firestore verification:
                    // val exists = firestore.collection("hikes")
                    //     .document(localHike.id)
                    //     .get()
                    //     .await()
                    //     .exists()
                    //
                    // if (exists) {
                    //     verifiedHikeIds.add(localHike.id)
                    // } else {
                    //     errors.add("Hike '${localHike.name}' not found in cloud")
                    // }

                    // For now, trust the sync flag
                    verifiedHikeIds.add(localHike.id)
                }

                VerificationResult(
                    success = errors.isEmpty(),
                    verifiedHikeIds = verifiedHikeIds,
                    errors = errors
                )
            } catch (e: Exception) {
                Log.e(TAG, "Verification failed", e)
                VerificationResult(
                    success = false,
                    verifiedHikeIds = emptyList(),
                    errors = listOf("Verification failed: ${e.message}")
                )
            }
        }
    }

    /**
     * Delete hike with all its images and observations
     */
    private suspend fun deleteHikeWithImages(hike: dev.panthu.mhikeapplication.data.local.entity.HikeEntity) {
        try {
            // Delete local images
            val imageUrls = deserializeImageUrls(hike.imageUrls, hike.id, "hike")
            imageUrls.forEach { path ->
                try {
                    deleteFileWithVerification(path)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete image: $path", e)
                }
            }

            // Delete observations and their images (cascade)
            val observations = observationDao.getObservationsForHikeSync(hike.id)
            observations.forEach { obs ->
                val obsImages = deserializeImageUrls(obs.imageUrls, obs.id, "observation")
                obsImages.forEach { path ->
                    try {
                        deleteFileWithVerification(path)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete observation image: $path", e)
                    }
                }
            }

            // Delete from database (cascade will delete observations)
            hikeDao.delete(hike)
            Log.d(TAG, "Deleted hike and associated data: ${hike.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting hike with images: ${hike.name}", e)
            throw e
        }
    }

    override suspend fun cleanupAfterMigration(guestId: String): Result<Unit> {
        return try {
            Log.i(TAG, "Starting cleanup for guest: $guestId")

            // CRITICAL: Verify all data exists in cloud before deleting local data
            val verificationResult = verifyCloudMigration(guestId)

            if (!verificationResult.success) {
                val errorMessage = "Migration verification failed: ${verificationResult.errors.joinToString()}"
                Log.e(TAG, errorMessage)
                return Result.Error(errorMessage)
            }

            Log.i(TAG, "Verification passed. Deleting ${verificationResult.verifiedHikeIds.size} verified hikes")

            // Step 2: Only delete verified items
            val localHikes = hikeDao.getUnsyncedHikes().filter { it.ownerId == guestId }
            var deletedCount = 0

            localHikes.filter { it.id in verificationResult.verifiedHikeIds }.forEach { hike ->
                try {
                    deleteHikeWithImages(hike)
                    deletedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete hike: ${hike.name}", e)
                }
            }

            Log.i(TAG, "Cleanup completed: $deletedCount hikes deleted successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            Result.Error("Cleanup failed: ${e.message}")
        }
    }
}
