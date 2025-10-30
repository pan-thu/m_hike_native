package dev.panthu.mhikeapplication.data.service

import android.net.Uri
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Named

/**
 * Implementation of MigrationService that handles migrating guest data to Firebase.
 */
class MigrationServiceImpl @Inject constructor(
    private val hikeDao: HikeDao,
    private val observationDao: ObservationDao,
    @Named("remote") private val remoteHikeRepository: HikeRepository,
    @Named("remote") private val remoteObservationRepository: ObservationRepository,
    private val imageRepository: ImageRepository,
    private val localImageRepository: LocalImageRepository
) : MigrationService {

    override fun migrateGuestData(
        guestId: String,
        newUserId: String
    ): Flow<MigrationProgress> = flow {
        val errors = mutableListOf<String>()
        var migratedHikes = 0
        var migratedObservations = 0
        var uploadedImages = 0

        try {
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
                    hikeEntity.imageUrls.let { urls ->
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(urls)
                    }.forEachIndexed { imgIndex, localPath ->
                        try {
                            emit(MigrationProgress.UploadingImages(
                                current = uploadedImages + 1,
                                total = stats.totalImages,
                                progress = (uploadedImages.toFloat() / stats.totalImages)
                            ))

                            val cloudUrl = uploadLocalImageToCloud(
                                localPath = localPath,
                                hikeId = hikeEntity.id,
                                newUserId = newUserId
                            )

                            if (cloudUrl != null) {
                                migratedImageUrls.add(cloudUrl)
                                uploadedImages++
                            } else {
                                errors.add("Failed to upload image for hike: ${hikeEntity.name}")
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
                            migratedObservations += migrateObservationsForHike(
                                hikeId = hikeEntity.id,
                                newUserId = newUserId,
                                stats = stats,
                                currentUploadedImages = uploadedImages,
                                onProgress = { progress ->
                                    emit(progress)
                                },
                                onImageUploaded = { uploadedImages++ },
                                onError = { errors.add(it) }
                            )
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

        } catch (e: Exception) {
            emit(MigrationProgress.Error(
                message = "Migration failed: ${e.message}",
                retryable = true
            ))
        }
    }

    private suspend fun migrateObservationsForHike(
        hikeId: String,
        newUserId: String,
        stats: MigrationStats,
        currentUploadedImages: Int,
        onProgress: suspend (MigrationProgress) -> Unit,
        onImageUploaded: () -> Unit,
        onError: (String) -> Unit
    ): Int {
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
                    obsEntity.imageUrls.let { urls ->
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(urls)
                    }.forEach { localPath ->
                        try {
                            onProgress(MigrationProgress.UploadingImages(
                                current = uploadedImages + 1,
                                total = stats.totalImages,
                                progress = (uploadedImages.toFloat() / stats.totalImages)
                            ))

                            val cloudUrl = uploadLocalImageToCloud(
                                localPath = localPath,
                                hikeId = hikeId,
                                observationId = obsEntity.id,
                                newUserId = newUserId
                            )

                            if (cloudUrl != null) {
                                migratedImageUrls.add(cloudUrl)
                                uploadedImages++
                                onImageUploaded()
                            } else {
                                onError("Failed to upload observation image")
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

        return migratedCount
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
                return null
            }

            val uri = Uri.fromFile(file)

            // Upload to Firebase Storage
            when (val result = imageRepository.uploadImage(uri, hikeId, observationId)) {
                is Result.Success -> result.data.url
                is Result.Error -> null
                is Result.Loading -> null
            }
        } catch (e: Exception) {
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
                // Count observations
                val observations = observationDao.getObservationsForHike(hike.id)
                totalObservations += observations.size

                // Count and size images for hike
                val hikeImages = kotlinx.serialization.json.Json.decodeFromString<List<String>>(hike.imageUrls)
                totalImages += hikeImages.size
                hikeImages.forEach { path ->
                    val file = File(path)
                    if (file.exists()) {
                        totalSizeBytes += file.length()
                    }
                }

                // Count and size images for observations
                observations.forEach { obs ->
                    val obsImages = kotlinx.serialization.json.Json.decodeFromString<List<String>>(obs.imageUrls)
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

    override suspend fun cleanupAfterMigration(guestId: String): Result<Unit> {
        return try {
            // Delete all synced local data for this guest
            val syncedHikes = hikeDao.getSyncedHikes(guestId)

            syncedHikes.forEach { hike ->
                // Delete local images
                val imageUrls = kotlinx.serialization.json.Json.decodeFromString<List<String>>(hike.imageUrls)
                imageUrls.forEach { path ->
                    try {
                        File(path).delete()
                    } catch (e: Exception) {
                        // Ignore individual file deletion errors
                    }
                }

                // Delete observations and their images
                val observations = observationDao.getObservationsForHikeSync(hike.id)
                observations.forEach { obs ->
                    val obsImages = kotlinx.serialization.json.Json.decodeFromString<List<String>>(obs.imageUrls)
                    obsImages.forEach { path ->
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            // Ignore individual file deletion errors
                        }
                    }
                }

                // Delete from database (cascade will delete observations)
                hikeDao.delete(hike)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Cleanup failed: ${e.message}")
        }
    }
}
