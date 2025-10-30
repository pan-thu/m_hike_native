package dev.panthu.mhikeapplication.data.local.repository

import dev.panthu.mhikeapplication.data.local.dao.ObservationDao
import dev.panthu.mhikeapplication.data.local.mapper.toDomain
import dev.panthu.mhikeapplication.data.local.mapper.toDomainObservationList
import dev.panthu.mhikeapplication.data.local.mapper.toEntity
import dev.panthu.mhikeapplication.domain.model.Observation
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Local implementation of ObservationRepository using Room database
 * Used in guest mode for offline-first functionality
 */
class LocalObservationRepositoryImpl @Inject constructor(
    private val observationDao: ObservationDao,
    private val localImageRepo: LocalImageRepository
) : ObservationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getObservationsForHike(hikeId: String): Flow<Result<List<Observation>>> {
        return observationDao.getObservationsForHike(hikeId)
            .map { entities -> Result.Success(entities.toDomainObservationList()) }
            .catch { emit(Result.Error(it.message ?: "Failed to load observations")) }
    }

    override fun getObservation(hikeId: String, observationId: String): Flow<Result<Observation?>> {
        return observationDao.getObservation(observationId)
            .map { entity -> Result.Success(entity?.toDomain()) }
            .catch { emit(Result.Error(it.message ?: "Failed to load observation")) }
    }

    override suspend fun createObservation(observation: Observation): Result<Observation> {
        return try {
            val entity = observation.toEntity()
            observationDao.insert(entity)
            Result.Success(observation)
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
            val entity = observationDao.getObservation(observationId).first()

            if (entity != null) {
                // Delete associated images
                val imageUrls = try {
                    json.decodeFromString<List<String>>(entity.imageUrls)
                } catch (e: Exception) {
                    emptyList()
                }

                imageUrls.forEach { localPath ->
                    localImageRepo.deleteImage(localPath)
                }

                // Delete observation directory
                localImageRepo.deleteDirectory("observations/$observationId")

                // Delete observation from database
                observationDao.deleteById(observationId)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete observation")
        }
    }

    override suspend fun createObservations(observations: List<Observation>): Result<List<Observation>> {
        return try {
            observations.forEach { observation ->
                val entity = observation.toEntity()
                observationDao.insert(entity)
            }
            Result.Success(observations)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create observations")
        }
    }
}
