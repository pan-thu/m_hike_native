package dev.panthu.mhikeapplication.domain.repository

import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.domain.model.Hike
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow

interface HikeRepository {

    /**
     * Get all hikes accessible to the current user (owned or shared)
     */
    fun getAllHikes(userId: String): Flow<Result<List<Hike>>>

    /**
     * Get a specific hike by ID with real-time updates
     */
    fun getHike(hikeId: String): Flow<Result<Hike?>>

    /**
     * Get hikes owned by the current user
     */
    fun getMyHikes(userId: String): Flow<Result<List<Hike>>>

    /**
     * Get hikes shared with the current user
     */
    fun getSharedHikes(userId: String): Flow<Result<List<Hike>>>

    /**
     * Create a new hike
     */
    suspend fun createHike(hike: Hike): Result<Hike>

    /**
     * Update an existing hike (owner only)
     */
    suspend fun updateHike(hike: Hike): Result<Hike>

    /**
     * Delete a hike (owner only)
     */
    suspend fun deleteHike(hikeId: String, userId: String): Result<Unit>

    /**
     * Share hike with a user (grants read access)
     */
    suspend fun shareHike(hikeId: String, userId: String): Result<Unit>

    /**
     * Revoke access for a user
     */
    suspend fun revokeAccess(hikeId: String, userId: String): Result<Unit>

    /**
     * Search hikes by name (prefix search)
     */
    suspend fun searchHikes(query: String, userId: String): Result<List<Hike>>

    /**
     * Filter hikes with advanced criteria
     */
    suspend fun filterHikes(
        userId: String,
        nameQuery: String? = null,
        locationQuery: String? = null,
        minLength: Double? = null,
        maxLength: Double? = null,
        startDate: com.google.firebase.Timestamp? = null,
        endDate: com.google.firebase.Timestamp? = null,
        difficulty: Difficulty? = null
    ): Result<List<Hike>>
}
