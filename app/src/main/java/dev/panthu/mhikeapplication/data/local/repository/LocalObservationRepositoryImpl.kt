package dev.panthu.mhikeapplication.data.local.repository

import android.util.Log
import androidx.room.withTransaction
import dev.panthu.mhikeapplication.data.local.MHikeDatabase
import dev.panthu.mhikeapplication.data.local.dao.ObservationDao
import dev.panthu.mhikeapplication.data.local.mapper.toDomain
import dev.panthu.mhikeapplication.data.local.mapper.toDomainObservationList
import dev.panthu.mhikeapplication.data.local.mapper.toEntity
import dev.panthu.mhikeapplication.domain.model.Observation
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Local implementation of ObservationRepository using Room database
 * Used in guest mode for offline-first functionality
 */
class LocalObservationRepositoryImpl @Inject constructor(
    private val database: MHikeDatabase,
    private val observationDao: ObservationDao,
    private val localImageRepo: LocalImageRepository
) : ObservationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getObservationsForHike(hikeId: String): Flow<Result<List<Observation>>> {
        return observationDao.getObservationsForHike(hikeId)
            .map<List<dev.panthu.mhikeapplication.data.local.entity.ObservationEntity>, Result<List<Observation>>> {
                entities -> Result.Success(entities.toDomainObservationList())
            }
            .catch { emit(Result.Error(it.message ?: "Failed to load observations")) }
    }

    override fun getObservation(hikeId: String, observationId: String): Flow<Result<Observation?>> {
        return observationDao.getObservation(observationId)
            .map<dev.panthu.mhikeapplication.data.local.entity.ObservationEntity?, Result<Observation?>> {
                entity -> Result.Success(entity?.toDomain())
            }
            .catch { emit(Result.Error(it.message ?: "Failed to load observation")) }
    }

    override suspend fun createObservation(observation: Observation): Result<Observation> {
        return try {
            // Generate UUID if ID is empty
            val observationWithId = if (observation.id.isEmpty()) {
                observation.copy(id = java.util.UUID.randomUUID().toString())
            } else {
                observation
            }
            val entity = observationWithId.toEntity()
            observationDao.insert(entity)
            Result.Success(observationWithId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create observation")
        }
    }

    override suspend fun updateObservation(observation: Observation): Result<Observation> {
        return try {
            val entity = observation.toEntity()
            observationDao.update(entity)
            Result.Success(observation)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update observation")
        }
    }

    override suspend fun deleteObservation(hikeId: String, observationId: String): Result<Unit> {
        return try {
            // Get observation to retrieve image URLs
            // Use firstOrNull() with timeout to avoid NoSuchElementException if observation doesn't exist
            // Timeout prevents indefinite blocking on slow database queries
            val entity = try {
                withTimeout(5.seconds) {
                    observationDao.getObservation(observationId).firstOrNull()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w("ObservationRepo", "Timeout while fetching observation $observationId for deletion")
                return Result.Error("Operation timed out: Unable to fetch observation data")
            }

            if (entity == null) {
                return Result.Error("Observation not found: $observationId")
            }

            // Parse imageUrls before transaction with comprehensive null safety
            val imageUrls = try {
                // Handle null or empty imageUrls field
                if (entity.imageUrls.isNullOrEmpty()) {
                    Log.d("ObservationRepo", "No imageUrls to parse for observation $observationId")
                    emptyList()
                } else {
                    val decoded = json.decodeFromString<List<String>>(entity.imageUrls)
                    // Filter out null or empty strings from the list
                    decoded.filterNotNull().filter { it.isNotEmpty() }
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.w("ObservationRepo", "Failed to deserialize imageUrls for observation $observationId: ${e.message}")
                emptyList()
            } catch (e: Exception) {
                Log.w("ObservationRepo", "Unexpected error parsing imageUrls for observation $observationId: ${e.message}")
                emptyList()
            }

            // Use transaction for atomic database operation
            database.withTransaction {
                // Delete observation from database
                observationDao.deleteById(observationId)
            }

            // File cleanup AFTER successful database transaction
            // (files can be orphaned but DB stays consistent - safer)
            try {
                // Null-safe iteration over imageUrls
                imageUrls.forEach { localPath ->
                    if (localPath.isNotEmpty()) {
                        localImageRepo.deleteImage(localPath)
                    }
                }

                // Delete observation directory
                localImageRepo.deleteDirectory("observations/$observationId")
            } catch (e: Exception) {
                // Log but don't fail - database is already consistent
                Log.w("ObservationRepo", "Failed to cleanup files for observation $observationId: ${e.message}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete observation")
        }
    }

    override suspend fun createObservations(observations: List<Observation>): Result<List<Observation>> {
        return try {
            // Pre-allocate lists with expected capacity for better memory efficiency
            val successfulObservations = ArrayList<Observation>(observations.size)
            val failedObservations = ArrayList<Pair<Observation, String>>()

            observations.forEach { observation ->
                try {
                    val entity = observation.toEntity()
                    observationDao.insert(entity)
                    successfulObservations.add(observation)
                } catch (e: Exception) {
                    val error = "Failed to create observation ${observation.id}: ${e.message}"
                    Log.e("ObservationRepo", error, e)
                    failedObservations.add(observation to error)
                }
            }

            // Return partial success information
            if (failedObservations.isEmpty()) {
                Result.Success(successfulObservations)
            } else {
                val errorMsg = buildString {
                    append("Batch operation completed with errors:\n")
                    append("Successful: ${successfulObservations.size}\n")
                    append("Failed: ${failedObservations.size}\n")
                    failedObservations.take(3).forEach { (obs, error) ->
                        append("- ${obs.text.take(30)}: $error\n")
                    }
                    if (failedObservations.size > 3) {
                        append("... and ${failedObservations.size - 3} more")
                    }
                }
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create observations")
        }
    }
}
