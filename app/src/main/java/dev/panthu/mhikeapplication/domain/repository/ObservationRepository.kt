package dev.panthu.mhikeapplication.domain.repository

import dev.panthu.mhikeapplication.domain.model.Observation
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow

interface ObservationRepository {

    /**
     * Get all observations for a hike with real-time updates
     */
    fun getObservationsForHike(hikeId: String): Flow<Result<List<Observation>>>

    /**
     * Get a specific observation by ID
     */
    fun getObservation(hikeId: String, observationId: String): Flow<Result<Observation?>>

    /**
     * Create a new observation
     */
    suspend fun createObservation(observation: Observation): Result<Observation>

    /**
     * Update an existing observation
     */
    suspend fun updateObservation(observation: Observation): Result<Observation>

    /**
     * Delete an observation
     */
    suspend fun deleteObservation(hikeId: String, observationId: String): Result<Unit>

    /**
     * Batch create observations (useful for creating during hike creation)
     */
    suspend fun createObservations(observations: List<Observation>): Result<List<Observation>>
}
