package dev.panthu.mhikeapplication.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.domain.usecase.SignInUseCase
import dev.panthu.mhikeapplication.domain.usecase.SignOutUseCase
import dev.panthu.mhikeapplication.domain.usecase.SignUpUseCase
import dev.panthu.mhikeapplication.util.GuestIdManager
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository,
    private val guestIdManager: GuestIdManager,
    private val migrationService: dev.panthu.mhikeapplication.domain.service.MigrationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        initializeAuthState()
    }

    /**
     * Initialize authentication state based on saved preferences and Firebase auth
     */
    private fun initializeAuthState() {
        viewModelScope.launch {
            // Check last auth mode
            val lastAuthMode = guestIdManager.getLastAuthMode()

            when (lastAuthMode) {
                GuestIdManager.AuthMode.GUEST -> {
                    // Restore guest session
                    val guestId = guestIdManager.getOrCreateGuestId()
                    _uiState.update {
                        it.copy(authState = AuthenticationState.Guest(guestId))
                    }
                }
                GuestIdManager.AuthMode.AUTHENTICATED -> {
                    // Check if Firebase session is still active
                    observeAuthState()
                }
                GuestIdManager.AuthMode.NONE -> {
                    // New user - show onboarding
                    _uiState.update {
                        it.copy(authState = AuthenticationState.Unauthenticated)
                    }
                }
            }
        }
    }

    /**
     * Observe Firebase auth state for authenticated users
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(authState = AuthenticationState.Authenticated(user))
                    }
                    guestIdManager.setLastAuthMode(GuestIdManager.AuthMode.AUTHENTICATED)
                } else {
                    // Firebase session expired, return to unauthenticated
                    _uiState.update {
                        it.copy(authState = AuthenticationState.Unauthenticated)
                    }
                    guestIdManager.setLastAuthMode(GuestIdManager.AuthMode.NONE)
                }
            }
        }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SignUp -> signUp(
                email = event.email,
                password = event.password,
                confirmPassword = event.confirmPassword,
                displayName = event.displayName,
                handle = event.handle
            )
            is AuthEvent.SignIn -> signIn(
                email = event.email,
                password = event.password
            )
            is AuthEvent.ContinueAsGuest -> continueAsGuest()
            is AuthEvent.SignOut -> signOut()
            is AuthEvent.ClearError -> clearError()
            is AuthEvent.DismissMigrationDialog -> dismissMigrationDialog()
            is AuthEvent.RetryMigration -> retryMigration()
        }
    }

    /**
     * Handle guest mode selection
     */
    private fun continueAsGuest() {
        viewModelScope.launch {
            val guestId = guestIdManager.getOrCreateGuestId()
            guestIdManager.setLastAuthMode(GuestIdManager.AuthMode.GUEST)
            guestIdManager.setOnboardingComplete()

            _uiState.update {
                it.copy(
                    authState = AuthenticationState.Guest(guestId),
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    private fun signUp(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String,
        handle: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Check if user was guest (for migration)
            val wasGuest = _uiState.value.isGuest
            val guestId = if (wasGuest) _uiState.value.guestId else null

            when (val result = signUpUseCase(email, password, confirmPassword, displayName, handle)) {
                is Result.Success -> {
                    val newUser = result.data

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authState = AuthenticationState.Authenticated(newUser)
                        )
                    }
                    guestIdManager.setLastAuthMode(GuestIdManager.AuthMode.AUTHENTICATED)
                    guestIdManager.setOnboardingComplete()

                    // Trigger migration if user was guest
                    if (wasGuest && guestId != null) {
                        guestIdManager.markAsFormerGuest()
                        startMigration(guestId, newUser.uid)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Sign up failed"
                        )
                    }
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = signInUseCase(email, password)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authState = AuthenticationState.Authenticated(result.data)
                        )
                    }
                    guestIdManager.setLastAuthMode(GuestIdManager.AuthMode.AUTHENTICATED)
                    guestIdManager.setOnboardingComplete()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Sign in failed"
                        )
                    }
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = signOutUseCase()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authState = AuthenticationState.Unauthenticated
                        )
                    }
                    guestIdManager.setLastAuthMode(GuestIdManager.AuthMode.NONE)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Sign out failed"
                        )
                    }
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Start guest data migration to cloud
     */
    private fun startMigration(guestId: String, newUserId: String) {
        viewModelScope.launch {
            // First check if migration is needed
            when (val statsResult = migrationService.checkMigrationNeeded(guestId)) {
                is Result.Success -> {
                    if (statsResult.data.isEmpty) {
                        // No data to migrate
                        return@launch
                    }

                    // Show migration dialog
                    _uiState.update {
                        it.copy(showMigrationDialog = true)
                    }

                    // Start migration
                    migrationService.migrateGuestData(guestId, newUserId).collect { progress ->
                        _uiState.update {
                            it.copy(migrationProgress = progress)
                        }

                        // On completion, cleanup local data
                        if (progress is dev.panthu.mhikeapplication.domain.service.MigrationProgress.Complete &&
                            progress.result.isSuccessful
                        ) {
                            migrationService.cleanupAfterMigration(guestId)
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            migrationProgress = dev.panthu.mhikeapplication.domain.service.MigrationProgress.Error(
                                message = "Failed to check migration data: ${statsResult.message}",
                                retryable = true
                            ),
                            showMigrationDialog = true
                        )
                    }
                }
                is Result.Loading -> {
                    // No action needed
                }
            }
        }
    }

    /**
     * Retry failed migration
     */
    private fun retryMigration() {
        val guestId = guestIdManager.getGuestId()
        val currentUser = _uiState.value.currentUser

        if (guestId != null && currentUser != null) {
            _uiState.update {
                it.copy(migrationProgress = null)
            }
            startMigration(guestId, currentUser.uid)
        }
    }

    /**
     * Dismiss migration dialog
     */
    private fun dismissMigrationDialog() {
        _uiState.update {
            it.copy(
                showMigrationDialog = false,
                migrationProgress = null
            )
        }
    }
}
