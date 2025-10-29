package dev.panthu.mhikeapplication.domain.model

import com.google.firebase.firestore.GeoPoint

data class Location(
    val name: String = "",
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0),
    val manualOverride: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "coordinates" to coordinates,
        "manualOverride" to manualOverride
    )

    companion object {
        fun fromMap(data: Map<String, Any>): Location {
            return Location(
                name = data["name"] as? String ?: "",
                coordinates = data["coordinates"] as? GeoPoint ?: GeoPoint(0.0, 0.0),
                manualOverride = data["manualOverride"] as? Boolean ?: false
            )
        }
    }
}
