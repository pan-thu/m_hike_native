package dev.panthu.mhikeapplication.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for local storage of observations in guest mode
 */
@Entity(
    tableName = "observations",
    foreignKeys = [
        ForeignKey(
            entity = HikeEntity::class,
            parentColumns = ["id"],
            childColumns = ["hikeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["hikeId"])]
)
data class ObservationEntity(
    @PrimaryKey
    val id: String,

    // Foreign key to hike
    val hikeId: String,

    // Required fields
    val text: String,
    val timestamp: Long, // Timestamp in milliseconds

    // Optional location
    val locationLat: Double?,
    val locationLng: Double?,

    // Images (JSON array of local paths)
    val imageUrls: String? = null, // JSON array (nullable for safety)

    // Optional comments
    val comments: String,

    // Timestamps
    val createdAt: Long,
    val updatedAt: Long,

    // Migration tracking
    val syncedToCloud: Boolean = false
)
