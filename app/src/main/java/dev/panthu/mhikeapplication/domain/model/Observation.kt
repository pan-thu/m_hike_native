package dev.panthu.mhikeapplication.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.UUID

data class Observation(
    val id: String = UUID.randomUUID().toString(),
    val hikeId: String = "",

    // Required field
    val text: String = "",

    // Time (defaults to now)
    val timestamp: Timestamp = Timestamp.now(),

    // Optional location
    val location: GeoPoint? = null,

    // Optional comments
    val comments: String = "",

    // Images
    val imageUrls: List<String> = emptyList(),

    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "hikeId" to hikeId,
        "text" to text,
        "timestamp" to timestamp,
        "location" to location,
        "comments" to comments,
        "imageUrls" to imageUrls,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    fun hasLocation(): Boolean = location != null

    fun hasImages(): Boolean = imageUrls.isNotEmpty()

    companion object {
        fun fromMap(data: Map<String, Any?>): Observation {
            return Observation(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                hikeId = data["hikeId"] as? String ?: "",
                text = data["text"] as? String ?: "",
                timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now(),
                location = data["location"] as? GeoPoint,
                comments = data["comments"] as? String ?: "",
                imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}
