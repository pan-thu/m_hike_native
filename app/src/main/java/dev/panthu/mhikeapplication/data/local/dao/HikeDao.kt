package dev.panthu.mhikeapplication.data.local.dao

import androidx.room.*
import dev.panthu.mhikeapplication.data.local.entity.HikeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for hikes table
 */
@Dao
interface HikeDao {

    /**
     * Get all hikes for a user (guest ID or user ID)
     */
    @Query("SELECT * FROM hikes WHERE ownerId = :userId ORDER BY date DESC")
    fun getAllHikes(userId: String): Flow<List<HikeEntity>>

    /**
     * Get a specific hike by ID
     */
    @Query("SELECT * FROM hikes WHERE id = :hikeId")
    fun getHike(hikeId: String): Flow<HikeEntity?>

    /**
     * Get owned hikes (same as getAllHikes for local storage)
     */
    @Query("SELECT * FROM hikes WHERE ownerId = :userId ORDER BY date DESC")
    fun getMyHikes(userId: String): Flow<List<HikeEntity>>

    /**
     * Insert a new hike
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hike: HikeEntity)

    /**
     * Update an existing hike
     */
    @Update
    suspend fun update(hike: HikeEntity)

    /**
     * Delete a hike
     */
    @Delete
    suspend fun delete(hike: HikeEntity)

    /**
     * Delete a hike by ID
     */
    @Query("DELETE FROM hikes WHERE id = :hikeId")
    suspend fun deleteById(hikeId: String)

    /**
     * Search hikes by name (prefix search)
     */
    @Query("SELECT * FROM hikes WHERE ownerId = :userId AND name LIKE :query || '%' ORDER BY date DESC")
    suspend fun searchByName(userId: String, query: String): List<HikeEntity>

    /**
     * Filter hikes with advanced criteria
     */
    @Query("""
        SELECT * FROM hikes
        WHERE ownerId = :userId
        AND (:nameQuery IS NULL OR name LIKE '%' || :nameQuery || '%')
        AND (:locationQuery IS NULL OR locationName LIKE '%' || :locationQuery || '%')
        AND (:minLength IS NULL OR length >= :minLength)
        AND (:maxLength IS NULL OR length <= :maxLength)
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:difficulty IS NULL OR difficulty = :difficulty)
        ORDER BY date DESC
    """)
    suspend fun filterHikes(
        userId: String,
        nameQuery: String?,
        locationQuery: String?,
        minLength: Double?,
        maxLength: Double?,
        startDate: Long?,
        endDate: Long?,
        difficulty: String?
    ): List<HikeEntity>

    /**
     * Get all unsynced hikes (for migration)
     */
    @Query("SELECT * FROM hikes WHERE syncedToCloud = 0")
    suspend fun getUnsyncedHikes(): List<HikeEntity>

    /**
     * Mark hike as synced
     */
    @Query("UPDATE hikes SET syncedToCloud = 1 WHERE id = :hikeId")
    suspend fun markAsSynced(hikeId: String)

    /**
     * Get all synced hikes for cleanup after migration
     */
    @Query("SELECT * FROM hikes WHERE ownerId = :userId AND syncedToCloud = 1")
    suspend fun getSyncedHikes(userId: String): List<HikeEntity>
}
