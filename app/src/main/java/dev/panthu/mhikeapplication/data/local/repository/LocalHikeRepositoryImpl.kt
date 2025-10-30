package dev.panthu.mhikeapplication.data.local.repository

import com.google.firebase.Timestamp
import dev.panthu.mhikeapplication.data.local.dao.HikeDao
import dev.panthu.mhikeapplication.data.local.mapper.toDomain
import dev.panthu.mhikeapplication.data.local.mapper.toDomainList
import dev.panthu.mhikeapplication.data.local.mapper.toEntity
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.domain.model.Hike
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Local implementation of HikeRepository using Room database
 * Used in guest mode for offline-first functionality
 */
class LocalHikeRepositoryImpl @Inject constructor(
    private val hikeDao: HikeDao,
    private val localImageRepo: LocalImageRepository
) : HikeRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllHikes(userId: String): Flow<Result<List<Hike>>> {
        return hikeDao.getAllHikes(userId)
            .map { entities -> Result.Success(entities.toDomainList()) }
            .catch { emit(Result.Error(it.message ?: "Failed to load hikes")) }
    }

    override fun getHike(hikeId: String): Flow<Result<Hike?>> {
        return hikeDao.getHike(hikeId)
            .map { entity -> Result.Success(entity?.toDomain()) }
            .catch { emit(Result.Error(it.message ?: "Failed to load hike")) }
    }

    override fun getMyHikes(userId: String): Flow<Result<List<Hike>>> {
        return hikeDao.getMyHikes(userId)
            .map { entities -> Result.Success(entities.toDomainList()) }
            .catch { emit(Result.Error(it.message ?: "Failed to load your hikes")) }
    }

    override fun getSharedHikes(userId: String): Flow<Result<List<Hike>>> {
        // Guest mode has no shared hikes
        return kotlinx.coroutines.flow.flowOf(Result.Success(emptyList()))
    }

    override suspend fun createHike(hike: Hike): Result<Hike> {
        return try {
            val entity = hike.toEntity()
            hikeDao.insert(entity)
            Result.Success(hike)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create hike")
        }
    }

    override suspend fun updateHike(hike: Hike): Result<Hike> {
        return try {
            val entity = hike.toEntity()
            hikeDao.update(entity)
            Result.Success(hike)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update hike")
        }
    }

    override suspend fun deleteHike(hikeId: String, userId: String): Result<Unit> {
        return try {
            // Get hike to retrieve image URLs
            val entity = hikeDao.getHike(hikeId).first()

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

                // Delete cover image if different
                if (entity.coverImageUrl.isNotEmpty() && !imageUrls.contains(entity.coverImageUrl)) {
                    localImageRepo.deleteImage(entity.coverImageUrl)
                }

                // Delete hike directory (cascade deletes observations due to foreign key)
                localImageRepo.deleteDirectory("hikes/$hikeId")

                // Delete hike from database
                hikeDao.deleteById(hikeId)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete hike")
        }
    }

    override suspend fun addInvitedUsers(hikeId: String, userIds: List<String>): Result<Unit> {
        return Result.Error("Sign up to invite users to your hikes")
    }

    override suspend fun shareHike(hikeId: String, userId: String): Result<Unit> {
        return Result.Error("Sign up to share hikes with others")
    }

    override suspend fun revokeAccess(hikeId: String, userId: String): Result<Unit> {
        return Result.Error("Sign up to manage hike access")
    }

    override suspend fun searchHikes(query: String, userId: String): Result<List<Hike>> {
        return try {
            val entities = hikeDao.searchByName(userId, query)
            Result.Success(entities.toDomainList())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to search hikes")
        }
    }

    override suspend fun filterHikes(
        userId: String,
        nameQuery: String?,
        locationQuery: String?,
        minLength: Double?,
        maxLength: Double?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        difficulty: Difficulty?
    ): Result<List<Hike>> {
        return try {
            val entities = hikeDao.filterHikes(
                userId = userId,
                nameQuery = nameQuery,
                locationQuery = locationQuery,
                minLength = minLength,
                maxLength = maxLength,
                startDate = startDate?.toDate()?.time,
                endDate = endDate?.toDate()?.time,
                difficulty = difficulty?.name
            )
            Result.Success(entities.toDomainList())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to filter hikes")
        }
    }
}
