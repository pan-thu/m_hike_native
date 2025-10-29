package dev.panthu.mhikeapplication.domain.repository

import android.location.Location
import dev.panthu.mhikeapplication.util.Result

interface LocationRepository {
    /**
     * Get current device location
     * Requires location permission to be granted
     */
    suspend fun getCurrentLocation(): Result<Location>

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean

    /**
     * Reverse geocode coordinates to place name
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String>
}
