package dev.panthu.mhikeapplication.data.local.mapper

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import dev.panthu.mhikeapplication.data.local.entity.HikeEntity
import dev.panthu.mhikeapplication.data.local.entity.ObservationEntity
import dev.panthu.mhikeapplication.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

/**
 * Extension functions to map between Room entities and domain models
 */

private const val TAG = "EntityMapper"
private val json = Json { ignoreUnknownKeys = true }

// ========== Hike Mappings ==========

/**
 * Convert HikeEntity (Room) to Hike (Domain)
 */
fun HikeEntity.toDomain(): Hike {
    return Hike(
        id = id,
        ownerId = ownerId,
        name = name,
        location = Location(
            name = locationName,
            coordinates = if (locationLat != null && locationLng != null) {
                GeoPoint(locationLat, locationLng)
            } else GeoPoint(0.0, 0.0)
        ),
        date = Timestamp(Date(date)),
        length = length,
        difficulty = Difficulty.fromString(difficulty),
        hasParking = hasParking,
        description = description,
        terrain = terrain,
        groupSize = groupSize,
        coverImageUrl = coverImageUrl,
        imageUrls = try {
            if (imageUrls.isNullOrEmpty()) {
                emptyList()
            } else {
                val decoded = json.decodeFromString<List<String>>(imageUrls)
                // Filter out null or empty strings for safety
                decoded.filterNotNull().filter { it.isNotEmpty() }
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e(TAG, "Serialization error deserializing imageUrls for hike $id", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error deserializing imageUrls for hike $id: ${imageUrls?.take(100)}", e)
            emptyList()
        },
        accessControl = AccessControl(
            invitedUsers = emptyList(), // Local storage doesn't have social features
            sharedUsers = emptyList()
        ),
        createdAt = Timestamp(Date(createdAt)),
        updatedAt = Timestamp(Date(updatedAt))
    )
}

/**
 * Convert Hike (Domain) to HikeEntity (Room)
 */
fun Hike.toEntity(): HikeEntity {
    // Filter out null or empty imageUrls before serialization
    val validImageUrls = imageUrls.filterNotNull().filter { it.isNotEmpty() }

    return HikeEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        locationName = location.name,
        locationLat = location.coordinates?.latitude,
        locationLng = location.coordinates?.longitude,
        date = date.toDate().time,
        length = length,
        difficulty = difficulty.name,
        hasParking = hasParking,
        description = description,
        terrain = terrain,
        groupSize = groupSize,
        coverImageUrl = coverImageUrl,
        imageUrls = try {
            json.encodeToString(validImageUrls)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize imageUrls for hike $id, using empty list", e)
            json.encodeToString(emptyList<String>())
        },
        createdAt = createdAt.toDate().time,
        updatedAt = updatedAt.toDate().time,
        syncedToCloud = false
    )
}

// ========== Observation Mappings ==========

/**
 * Convert ObservationEntity (Room) to Observation (Domain)
 */
fun ObservationEntity.toDomain(): Observation {
    return Observation(
        id = id,
        hikeId = hikeId,
        text = text,
        timestamp = Timestamp(Date(timestamp)),
        location = if (locationLat != null && locationLng != null) {
            GeoPoint(locationLat, locationLng)
        } else null,
        imageUrls = try {
            if (imageUrls.isNullOrEmpty()) {
                emptyList()
            } else {
                val decoded = json.decodeFromString<List<String>>(imageUrls)
                // Filter out null or empty strings for safety
                decoded.filterNotNull().filter { it.isNotEmpty() }
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e(TAG, "Serialization error deserializing imageUrls for observation $id", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error deserializing imageUrls for observation $id: ${imageUrls?.take(100)}", e)
            emptyList()
        },
        comments = comments,
        createdAt = Timestamp(Date(createdAt)),
        updatedAt = Timestamp(Date(updatedAt))
    )
}

/**
 * Convert Observation (Domain) to ObservationEntity (Room)
 */
fun Observation.toEntity(): ObservationEntity {
    // Filter out null or empty imageUrls before serialization
    val validImageUrls = imageUrls.filterNotNull().filter { it.isNotEmpty() }

    return ObservationEntity(
        id = id,
        hikeId = hikeId,
        text = text,
        timestamp = timestamp.toDate().time,
        locationLat = location?.latitude,
        locationLng = location?.longitude,
        imageUrls = try {
            json.encodeToString(validImageUrls)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize imageUrls for observation $id, using empty list", e)
            json.encodeToString(emptyList<String>())
        },
        comments = comments,
        createdAt = createdAt.toDate().time,
        updatedAt = updatedAt.toDate().time,
        syncedToCloud = false
    )
}

// ========== Helper Functions ==========

/**
 * Convert list of HikeEntity to list of Hike
 * Safely handles conversion errors by filtering out failed conversions
 */
fun List<HikeEntity>.toDomainList(): List<Hike> {
    return mapNotNull { entity ->
        try {
            entity.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert HikeEntity ${entity.id} to domain model, skipping", e)
            null
        }
    }
}

/**
 * Convert list of ObservationEntity to list of Observation
 * Safely handles conversion errors by filtering out failed conversions
 */
fun List<ObservationEntity>.toDomainObservationList(): List<Observation> {
    return mapNotNull { entity ->
        try {
            entity.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert ObservationEntity ${entity.id} to domain model, skipping", e)
            null
        }
    }
}
