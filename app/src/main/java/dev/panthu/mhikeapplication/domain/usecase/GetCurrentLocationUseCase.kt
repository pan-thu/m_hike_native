package dev.panthu.mhikeapplication.domain.usecase

import android.location.Location
import com.google.firebase.firestore.GeoPoint
import dev.panthu.mhikeapplication.domain.repository.LocationRepository
import dev.panthu.mhikeapplication.util.Result
import javax.inject.Inject

data class LocationResult(
    val geoPoint: GeoPoint,
    val placeName: String,
    val androidLocation: Location
)

class GetCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Result<LocationResult> {
        // Check permission first
        if (!locationRepository.hasLocationPermission()) {
            return Result.Error(
                SecurityException("Location permission not granted"),
                "Please grant location permission to use this feature"
            )
        }

        // Get current location
        return when (val locationResult = locationRepository.getCurrentLocation()) {
            is Result.Success -> {
                val location = locationResult.data
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                // Try to get place name (non-blocking failure)
                val placeName = when (val geocodeResult = locationRepository.reverseGeocode(
                    location.latitude,
                    location.longitude
                )) {
                    is Result.Success -> geocodeResult.data
                    else -> "Lat: ${String.format("%.4f", location.latitude)}, " +
                            "Long: ${String.format("%.4f", location.longitude)}"
                }

                Result.Success(
                    LocationResult(
                        geoPoint = geoPoint,
                        placeName = placeName,
                        androidLocation = location
                    )
                )
            }
            is Result.Error -> Result.Error(
                locationResult.exception,
                locationResult.message ?: "Failed to get location"
            )
            is Result.Loading -> Result.Loading
        }
    }
}
