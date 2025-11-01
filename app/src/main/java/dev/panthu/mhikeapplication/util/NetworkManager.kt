package dev.panthu.mhikeapplication.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetworkManager - Utility class for network connectivity monitoring
 *
 * Provides real-time network status monitoring and connectivity checks
 * for Firebase operations and other network-dependent features.
 *
 * Features:
 * - Real-time network status via Flow
 * - Synchronous connectivity checks
 * - Android 6+ (API 23+) support with fallback
 * - Thread-safe singleton implementation
 */
@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Network status data class
     * @param isConnected Whether device has active network connection
     * @param isMetered Whether connection is metered (mobile data, paid Wi-Fi)
     * @param networkType Type of network (Wi-Fi, Cellular, etc.)
     */
    data class NetworkStatus(
        val isConnected: Boolean,
        val isMetered: Boolean = false,
        val networkType: NetworkType = NetworkType.UNKNOWN
    )

    /**
     * Network type enumeration
     */
    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN,
        UNKNOWN
    }

    /**
     * Check if device has active network connection
     *
     * @return true if connected to any network, false otherwise
     */
    fun isNetworkAvailable(): Boolean {
        if (connectivityManager == null) {
            android.util.Log.w(TAG, "ConnectivityManager not available")
            return false
        }

        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error checking network availability", e)
            false
        }
    }

    /**
     * Get current network status with detailed information
     *
     * @return NetworkStatus with connection state and network type
     */
    fun getNetworkStatus(): NetworkStatus {
        if (connectivityManager == null) {
            return NetworkStatus(isConnected = false)
        }

        return try {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                return NetworkStatus(isConnected = false)
            }

            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                return NetworkStatus(isConnected = false)
            }

            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                             capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val isMetered = connectivityManager.isActiveNetworkMetered

            val networkType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
                else -> NetworkType.UNKNOWN
            }

            NetworkStatus(
                isConnected = isConnected,
                isMetered = isMetered,
                networkType = networkType
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting network status", e)
            NetworkStatus(isConnected = false)
        }
    }

    /**
     * Observe network connectivity changes as a Flow
     *
     * Emits NetworkStatus whenever network state changes.
     * Automatically unregisters callback when Flow is cancelled.
     *
     * @return Flow<NetworkStatus> that emits on network changes
     */
    fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
        if (connectivityManager == null) {
            trySend(NetworkStatus(isConnected = false))
            close()
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getNetworkStatus())
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus(isConnected = false))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getNetworkStatus())
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            // Send initial state
            trySend(getNetworkStatus())

            awaitClose {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Error unregistering network callback", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error registering network callback", e)
            trySend(NetworkStatus(isConnected = false))
            close()
        }
    }.distinctUntilChanged()

    /**
     * Check if network is available for Firebase operations
     *
     * Convenience method that checks for internet connectivity
     * and logs warning if not available.
     *
     * @param operation Name of operation for logging (e.g., "Firestore query")
     * @return true if network is available, false otherwise
     */
    fun requireNetwork(operation: String): Boolean {
        val available = isNetworkAvailable()
        if (!available) {
            android.util.Log.w(TAG, "Network not available for: $operation")
        }
        return available
    }

    /**
     * Execute network operation with automatic retry on connection restore
     *
     * Waits for network if not currently available, then executes operation.
     * Useful for Firebase operations that need guaranteed connectivity.
     *
     * @param operation Suspend function to execute when network is available
     * @return Result of operation
     */
    suspend fun <T> executeWithNetwork(
        operation: suspend () -> Result<T>
    ): Result<T> {
        if (!isNetworkAvailable()) {
            return Result.Error("No network connection available")
        }

        return try {
            operation()
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network operation failed")
        }
    }

    companion object {
        private const val TAG = "NetworkManager"
    }
}
