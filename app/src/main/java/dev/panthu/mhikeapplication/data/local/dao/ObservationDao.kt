package dev.panthu.mhikeapplication.data.local.dao

import androidx.room.*
import dev.panthu.mhikeapplication.data.local.entity.ObservationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for observations table
 */
@Dao
interface ObservationDao {

    /**
     * Get all observations for a hike (Flow)
     */
    @Query("SELECT * FROM observations WHERE hikeId = :hikeId ORDER BY timestamp DESC")
    fun getObservationsForHike(hikeId: String): Flow<List<ObservationEntity>>

    /**
     * Get all observations for a hike (suspend for migration)
     */
    @Query("SELECT * FROM observations WHERE hikeId = :hikeId ORDER BY timestamp DESC")
    suspend fun getObservationsForHikeSync(hikeId: String): List<ObservationEntity>

    /**
     * Get a specific observation by ID
     */
    @Query("SELECT * FROM observations WHERE id = :observationId")
    fun getObservation(observationId: String): Flow<ObservationEntity?>

    /**
     * Insert a new observation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: ObservationEntity)

    /**
     * Update an existing observation
     */
    @Update
    suspend fun update(observation: ObservationEntity)

    /**
     * Delete an observation
     */
    @Delete
    suspend fun delete(observation: ObservationEntity)

    /**
     * Delete an observation by ID
     */
    @Query("DELETE FROM observations WHERE id = :observationId")
    suspend fun deleteById(observationId: String)

    /**
     * Delete all observations for a hike (cascading delete)
     */
    @Query("DELETE FROM observations WHERE hikeId = :hikeId")
    suspend fun deleteAllForHike(hikeId: String)

    /**
     * Get all unsynced observations (for migration)
     */
    @Query("SELECT * FROM observations WHERE syncedToCloud = 0")
    suspend fun getUnsyncedObservations(): List<ObservationEntity>

    /**
     * Mark observation as synced
     */
    @Query("UPDATE observations SET syncedToCloud = 1 WHERE id = :observationId")
    suspend fun markAsSynced(observationId: String)

    /**
     * Get observation count for a hike
     */
    @Query("SELECT COUNT(*) FROM observations WHERE hikeId = :hikeId")
    suspend fun getObservationCount(hikeId: String): Int
}
