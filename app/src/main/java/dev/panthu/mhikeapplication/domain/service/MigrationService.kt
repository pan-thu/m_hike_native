package dev.panthu.mhikeapplication.domain.service

import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Service responsible for migrating guest user data to cloud storage
 * when they sign up for an authenticated account.
 */
interface MigrationService {

    /**
     * Migrate all guest data to cloud storage.
     *
     * @param guestId The guest user ID whose data should be migrated
     * @param newUserId The authenticated user ID to migrate data to
     * @return Flow of migration progress events
     */
    fun migrateGuestData(
        guestId: String,
        newUserId: String
    ): Flow<MigrationProgress>

    /**
     * Check if there is any unsynced guest data that needs migration.
     *
     * @param guestId The guest user ID to check
     * @return Result containing migration statistics
     */
    suspend fun checkMigrationNeeded(guestId: String): Result<MigrationStats>

    /**
     * Delete all local guest data after successful migration.
     *
     * @param guestId The guest user ID whose data should be cleaned up
     * @return Result indicating success or failure
     */
    suspend fun cleanupAfterMigration(guestId: String): Result<Unit>
}

/**
 * Represents the current state and progress of data migration.
 */
sealed class MigrationProgress {
    data class Initializing(val stats: MigrationStats) : MigrationProgress()

    data class MigratingHikes(
        val current: Int,
        val total: Int,
        val hikeName: String
    ) : MigrationProgress()

    data class MigratingObservations(
        val current: Int,
        val total: Int,
        val hikeId: String
    ) : MigrationProgress()

    data class UploadingImages(
        val current: Int,
        val total: Int,
        val progress: Float
    ) : MigrationProgress()

    data class Complete(val result: MigrationResult) : MigrationProgress()

    data class Error(val message: String, val retryable: Boolean = true) : MigrationProgress()
}

/**
 * Statistics about data that needs migration.
 */
data class MigrationStats(
    val totalHikes: Int,
    val totalObservations: Int,
    val totalImages: Int,
    val estimatedSizeBytes: Long
) {
    val isEmpty: Boolean
        get() = totalHikes == 0 && totalObservations == 0

    val estimatedSizeMB: Float
        get() = estimatedSizeBytes / (1024f * 1024f)
}

/**
 * Result of a completed migration operation.
 */
data class MigrationResult(
    val migratedHikes: Int,
    val migratedObservations: Int,
    val uploadedImages: Int,
    val failedItems: Int = 0,
    val errors: List<String> = emptyList()
) {
    val isSuccessful: Boolean
        get() = failedItems == 0

    val hasPartialSuccess: Boolean
        get() = failedItems > 0 && (migratedHikes > 0 || migratedObservations > 0)
}
