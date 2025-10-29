package dev.panthu.mhikeapplication.domain.model

import com.google.firebase.Timestamp
import java.util.UUID

data class Hike(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String = "",

    // Required fields (FR-01)
    val name: String = "",
    val location: Location = Location(),
    val date: Timestamp = Timestamp.now(),
    val length: Double = 0.0, // in kilometers
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val hasParking: Boolean = false,

    // Optional fields
    val description: String = "",
    val terrain: String = "",
    val groupSize: Int = 0,

    // Images
    val coverImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),

    // Access control
    val accessControl: AccessControl = AccessControl(),

    // Timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerId" to ownerId,
        "name" to name,
        "location" to location.toMap(),
        "date" to date,
        "length" to length,
        "difficulty" to difficulty.name,
        "hasParking" to hasParking,
        "description" to description,
        "terrain" to terrain,
        "groupSize" to groupSize,
        "coverImageUrl" to coverImageUrl,
        "imageUrls" to imageUrls,
        "accessControl" to accessControl.toMap(),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    fun isOwner(userId: String): Boolean = ownerId == userId

    fun hasReadAccess(userId: String): Boolean {
        return isOwner(userId) || accessControl.hasAccess(userId)
    }

    fun canEdit(userId: String): Boolean = isOwner(userId)

    companion object {
        fun fromMap(data: Map<String, Any?>): Hike {
            return Hike(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                ownerId = data["ownerId"] as? String ?: "",
                name = data["name"] as? String ?: "",
                location = (data["location"] as? Map<String, Any>)?.let { Location.fromMap(it) } ?: Location(),
                date = data["date"] as? Timestamp ?: Timestamp.now(),
                length = (data["length"] as? Number)?.toDouble() ?: 0.0,
                difficulty = (data["difficulty"] as? String)?.let { Difficulty.fromString(it) } ?: Difficulty.MEDIUM,
                hasParking = data["hasParking"] as? Boolean ?: false,
                description = data["description"] as? String ?: "",
                terrain = data["terrain"] as? String ?: "",
                groupSize = (data["groupSize"] as? Number)?.toInt() ?: 0,
                coverImageUrl = data["coverImageUrl"] as? String ?: "",
                imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                accessControl = (data["accessControl"] as? Map<String, Any>)?.let { AccessControl.fromMap(it) } ?: AccessControl(),
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
