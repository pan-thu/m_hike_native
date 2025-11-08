package dev.panthu.mhikeapplication.data.local.repository

import androidx.room.withTransaction
import com.google.firebase.Timestamp
import dev.panthu.mhikeapplication.data.local.MHikeDatabase
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Local implementation of HikeRepository using Room database
 * Used in guest mode for offline-first functionality
 */
class LocalHikeRepositoryImpl @Inject constructor(
    private val database: MHikeDatabase,
    private val hikeDao: HikeDao,
    private val localImageRepo: LocalImageRepository
) : HikeRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllHikes(userId: String): Flow<Result<List<Hike>>> {
        return hikeDao.getAllHikes(userId)
            .map<List<dev.panthu.mhikeapplication.data.local.entity.HikeEntity>, Result<List<Hike>>> {
                entities -> Result.Success(entities.toDomainList())
            }
            .catch { emit(Result.Error(it.message ?: "Failed to load hikes")) }
    }

    override fun getHike(hikeId: String): Flow<Result<Hike?>> {
        return hikeDao.getHike(hikeId)
            .map<dev.panthu.mhikeapplication.data.local.entity.HikeEntity?, Result<Hike?>> {
                entity -> Result.Success(entity?.toDomain())
            }
            .catch { emit(Result.Error(it.message ?: "Failed to load hike")) }
    }

    override fun getMyHikes(userId: String): Flow<Result<List<Hike>>> {
        return hikeDao.getMyHikes(userId)
            .map<List<dev.panthu.mhikeapplication.data.local.entity.HikeEntity>, Result<List<Hike>>> {
                entities -> Result.Success(entities.toDomainList())
            }
            .catch { emit(Result.Error(it.message ?: "Failed to load your hikes")) }
    }

    override fun getSharedHikes(userId: String): Flow<Result<List<Hike>>> {
        // Guest mode has no shared hikes
        return kotlinx.coroutines.flow.flowOf(Result.Success(emptyList()))
    }

    override suspend fun createHike(hike: Hike): Result<Hike> {
        return try {
            // Generate UUID if ID is empty
            val hikeWithId = if (hike.id.isEmpty()) {
                hike.copy(id = java.util.UUID.randomUUID().toString())
            } else {
                hike
            }
            val entity = hikeWithId.toEntity()
            hikeDao.insert(entity)
            Result.Success(hikeWithId)
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
            // Use firstOrNull() with timeout to avoid NoSuchElementException if hike doesn't exist
            // Timeout prevents indefinite blocking on slow database queries
            val entity = try {
                withTimeout(5.seconds) {
                    hikeDao.getHike(hikeId).firstOrNull()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.w("HikeRepository", "Timeout while fetching hike $hikeId for deletion")
                return Result.Error("Operation timed out: Unable to fetch hike data")
            }

            if (entity == null) {
                return Result.Error("Hike not found: $hikeId")
            }

            // Parse image URLs before transaction with comprehensive null safety
            val imageUrls = try {
                // Handle null or empty imageUrls field
                if (entity.imageUrls.isNullOrEmpty()) {
                    android.util.Log.d("HikeRepository", "No imageUrls to parse for hike $hikeId")
                    emptyList()
                } else {
                    val decoded = json.decodeFromString<List<String>>(entity.imageUrls)
                    // Filter out null or empty strings from the list
                    decoded.filterNotNull().filter { it.isNotEmpty() }
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                android.util.Log.w("HikeRepository", "Failed to deserialize imageUrls for hike $hikeId: ${e.message}")
                emptyList()
            } catch (e: Exception) {
                android.util.Log.w("HikeRepository", "Unexpected error parsing imageUrls for hike $hikeId: ${e.message}")
                emptyList()
            }

            // Use transaction for atomic database operations
            database.withTransaction {
                // Delete hike from database (cascade deletes observations due to foreign key)
                hikeDao.deleteById(hikeId)
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

                // Delete cover image if different with null safety
                val coverImageUrl = entity.coverImageUrl
                if (!coverImageUrl.isNullOrEmpty() && !imageUrls.contains(coverImageUrl)) {
                    localImageRepo.deleteImage(coverImageUrl)
                }

                // Delete hike directory
                localImageRepo.deleteDirectory("hikes/$hikeId")
            } catch (e: Exception) {
                // Log but don't fail - database is already consistent
                android.util.Log.w("HikeRepository", "Failed to cleanup files for hike $hikeId: ${e.message}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to delete hike")
        }
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
            // Validate input ranges
            if (minLength != null && maxLength != null && minLength > maxLength) {
                return Result.Error("Minimum length ($minLength km) cannot be greater than maximum length ($maxLength km)")
            }

            if (minLength != null && minLength < 0) {
                return Result.Error("Minimum length cannot be negative")
            }

            if (maxLength != null && maxLength < 0) {
                return Result.Error("Maximum length cannot be negative")
            }

            if (startDate != null && endDate != null) {
                val startMillis = startDate.toDate().time
                val endMillis = endDate.toDate().time
                if (startMillis > endMillis) {
                    return Result.Error("Start date cannot be after end date")
                }
            }

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
