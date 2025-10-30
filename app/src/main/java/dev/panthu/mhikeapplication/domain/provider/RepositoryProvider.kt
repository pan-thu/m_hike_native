package dev.panthu.mhikeapplication.domain.provider

import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import dev.panthu.mhikeapplication.presentation.auth.AuthViewModel
import dev.panthu.mhikeapplication.presentation.auth.AuthenticationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Strategy provider that selects repository implementation based on authentication state
 * - Guest/Unauthenticated → Local repositories (Room + File storage)
 * - Authenticated → Remote repositories (Firebase)
 */
interface RepositoryProvider {
    fun getHikeRepository(): HikeRepository
    fun getObservationRepository(): ObservationRepository
}

@Singleton
class DynamicRepositoryProvider @Inject constructor(
    @Named("local") private val localHikeRepo: HikeRepository,
    @Named("remote") private val remoteHikeRepo: HikeRepository,
    @Named("local") private val localObservationRepo: ObservationRepository,
    @Named("remote") private val remoteObservationRepo: ObservationRepository,
    private val authViewModel: AuthViewModel
) : RepositoryProvider {

    /**
     * Returns appropriate HikeRepository based on current auth state
     */
    override fun getHikeRepository(): HikeRepository {
        return when (getCurrentAuthState()) {
            is AuthenticationState.Authenticated -> remoteHikeRepo
            else -> localHikeRepo // Guest or Unauthenticated
        }
    }

    /**
     * Returns appropriate ObservationRepository based on current auth state
     */
    override fun getObservationRepository(): ObservationRepository {
        return when (getCurrentAuthState()) {
            is AuthenticationState.Authenticated -> remoteObservationRepo
            else -> localObservationRepo // Guest or Unauthenticated
        }
    }

    /**
     * Get current authentication state synchronously
     * This is safe because we're only reading the current value, not collecting
     */
    private fun getCurrentAuthState(): AuthenticationState {
        return runBlocking {
            authViewModel.uiState.first().authState
        }
    }
}
