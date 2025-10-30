package dev.panthu.mhikeapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for local storage of hikes in guest mode
 */
@Entity(tableName = "hikes")
data class HikeEntity(
    @PrimaryKey
    val id: String,

    // Owner
    val ownerId: String,

    // Required fields
    val name: String,

    // Location (flattened)
    val locationName: String,
    val locationLat: Double?,
    val locationLng: Double?,

    // Hike details
    val date: Long, // Timestamp in milliseconds
    val length: Double,
    val difficulty: String, // Difficulty enum as string
    val hasParking: Boolean,

    // Optional fields
    val description: String,
    val terrain: String,
    val groupSize: Int,

    // Images (JSON array of local paths)
    val coverImageUrl: String,
    val imageUrls: String, // JSON array

    // Timestamps
    val createdAt: Long,
    val updatedAt: Long,

    // Migration tracking
    val syncedToCloud: Boolean = false
)
