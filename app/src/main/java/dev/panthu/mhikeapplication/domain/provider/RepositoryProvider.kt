package dev.panthu.mhikeapplication.domain.provider

import android.util.Log
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import dev.panthu.mhikeapplication.presentation.auth.AuthViewModel
import dev.panthu.mhikeapplication.presentation.auth.AuthenticationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Strategy provider that selects repository implementation based on authentication state
 * - Guest/Unauthenticated → Local repositories (Room + File storage)
 * - Authenticated → Remote repositories (Firebase)
 *
 * Thread-safe implementation using StateFlow and Mutex to prevent race conditions
 */
interface RepositoryProvider {
    suspend fun getHikeRepository(): HikeRepository
    suspend fun getObservationRepository(): ObservationRepository
    fun getHikeRepositorySync(): HikeRepository
    fun getObservationRepositorySync(): ObservationRepository
}

@Singleton
class DynamicRepositoryProvider @Inject constructor(
    @Named("local") private val localHikeRepo: HikeRepository,
    @Named("remote") private val remoteHikeRepo: HikeRepository,
    @Named("local") private val localObservationRepo: ObservationRepository,
    @Named("remote") private val remoteObservationRepo: ObservationRepository,
    private val authViewModel: AuthViewModel
) : RepositoryProvider {

    companion object {
        private const val TAG = "RepositoryProvider"
    }

    // Mutex for thread-safe repository selection
    private val repositoryMutex = Mutex()

    // Use application scope for lifecycle management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // StateFlow for thread-safe auth state observation
    private val authState: StateFlow<AuthenticationState> = authViewModel.uiState
        .map { it.authState }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthenticationState.Unauthenticated
        )

    /**
     * Returns appropriate HikeRepository based on current auth state
     * Thread-safe with mutex lock to prevent race conditions
     */
    override suspend fun getHikeRepository(): HikeRepository = repositoryMutex.withLock {
        val currentState = authState.value
        Log.d(TAG, "Selecting hike repository for auth state: $currentState")

        when (currentState) {
            is AuthenticationState.Authenticated -> remoteHikeRepo
            else -> localHikeRepo // Guest or Unauthenticated
        }
    }

    /**
     * Returns appropriate ObservationRepository based on current auth state
     * Thread-safe with mutex lock to prevent race conditions
     */
    override suspend fun getObservationRepository(): ObservationRepository = repositoryMutex.withLock {
        val currentState = authState.value
        Log.d(TAG, "Selecting observation repository for auth state: $currentState")

        when (currentState) {
            is AuthenticationState.Authenticated -> remoteObservationRepo
            else -> localObservationRepo // Guest or Unauthenticated
        }
    }

    /**
     * Synchronous version for contexts that cannot use suspend functions
     * Note: May have slight delay if auth state is transitioning
     */
    override fun getHikeRepositorySync(): HikeRepository {
        val currentState = authState.value
        return when (currentState) {
            is AuthenticationState.Authenticated -> remoteHikeRepo
            else -> localHikeRepo
        }
    }

    /**
     * Synchronous version for contexts that cannot use suspend functions
     * Note: May have slight delay if auth state is transitioning
     */
    override fun getObservationRepositorySync(): ObservationRepository {
        val currentState = authState.value
        return when (currentState) {
            is AuthenticationState.Authenticated -> remoteObservationRepo
            else -> localObservationRepo
        }
    }
}
