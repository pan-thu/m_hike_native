package dev.panthu.mhikeapplication.data.local.mapper

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
            } else null
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
            json.decodeFromString<List<String>>(imageUrls)
        } catch (e: Exception) {
            emptyList()
        },
        accessControl = AccessControl(
            ownerId = ownerId,
            invitedUsers = emptyList(), // Local storage doesn't have social features
            sharedWith = emptyList()
        ),
        createdAt = Timestamp(Date(createdAt)),
        updatedAt = Timestamp(Date(updatedAt))
    )
}

/**
 * Convert Hike (Domain) to HikeEntity (Room)
 */
fun Hike.toEntity(): HikeEntity {
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
        imageUrls = json.encodeToString(imageUrls),
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
            json.decodeFromString<List<String>>(imageUrls)
        } catch (e: Exception) {
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
    return ObservationEntity(
        id = id,
        hikeId = hikeId,
        text = text,
        timestamp = timestamp.toDate().time,
        locationLat = location?.latitude,
        locationLng = location?.longitude,
        imageUrls = json.encodeToString(imageUrls),
        comments = comments,
        createdAt = createdAt.toDate().time,
        updatedAt = updatedAt.toDate().time,
        syncedToCloud = false
    )
}

// ========== Helper Functions ==========

/**
 * Convert list of HikeEntity to list of Hike
 */
fun List<HikeEntity>.toDomainList(): List<Hike> {
    return map { it.toDomain() }
}

/**
 * Convert list of ObservationEntity to list of Observation
 */
fun List<ObservationEntity>.toDomainObservationList(): List<Observation> {
    return map { it.toDomain() }
}
