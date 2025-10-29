package dev.panthu.mhikeapplication.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.panthu.mhikeapplication.domain.repository.LocationRepository
import dev.panthu.mhikeapplication.util.Result
import dev.panthu.mhikeapplication.util.safeCall
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) : LocationRepository {

    override suspend fun getCurrentLocation(): Result<Location> = safeCall {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }

        val cancellationTokenSource = CancellationTokenSource()

        try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            location ?: throw Exception("Unable to get current location")
        } catch (e: SecurityException) {
            throw SecurityException("Location permission not granted")
        } catch (e: Exception) {
            throw Exception("Failed to get location: ${e.message}")
        }
    }

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<String> = safeCall {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use new API for Android 13+
            suspendCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1
                ) { addresses ->
                    val placeName = addresses.firstOrNull()?.let { address ->
                        buildPlaceName(address)
                    } ?: "Unknown location"
                    continuation.resume(placeName)
                }
            }
        } else {
            // Use deprecated API for older versions
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val placeName = addresses?.firstOrNull()?.let { address ->
                buildPlaceName(address)
            } ?: "Unknown location"
            placeName
        }
    }

    private fun buildPlaceName(address: Address): String {
        return buildList {
            address.featureName?.let { add(it) }
            address.locality?.let { add(it) }
            address.adminArea?.let { add(it) }
        }.joinToString(", ").ifEmpty { "Unknown location" }
    }
}
