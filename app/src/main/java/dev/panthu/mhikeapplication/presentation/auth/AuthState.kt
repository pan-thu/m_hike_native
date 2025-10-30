package dev.panthu.mhikeapplication.presentation.auth

import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.domain.service.MigrationProgress

/**
 * Sealed class representing the three possible authentication states
 */
sealed class AuthenticationState {
    /** User has not made a choice yet (first launch) */
    object Unauthenticated : AuthenticationState()

    /** User chose to continue as guest with local-only storage */
    data class Guest(val guestId: String) : AuthenticationState()

    /** User authenticated with Firebase (full cloud features) */
    data class Authenticated(val user: User) : AuthenticationState()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val authState: AuthenticationState = AuthenticationState.Unauthenticated,
    val error: String? = null,
    val migrationProgress: MigrationProgress? = null,
    val showMigrationDialog: Boolean = false
) {
    /** Convenience properties for backward compatibility */
    val isAuthenticated: Boolean
        get() = authState is AuthenticationState.Authenticated

    val isGuest: Boolean
        get() = authState is AuthenticationState.Guest

    val currentUser: User?
        get() = (authState as? AuthenticationState.Authenticated)?.user

    val guestId: String?
        get() = (authState as? AuthenticationState.Guest)?.guestId

    val isMigrating: Boolean
        get() = migrationProgress != null &&
                migrationProgress !is MigrationProgress.Complete &&
                migrationProgress !is MigrationProgress.Error
}

sealed class AuthEvent {
    data class SignUp(
        val email: String,
        val password: String,
        val confirmPassword: String,
        val displayName: String,
        val handle: String
    ) : AuthEvent()

    data class SignIn(
        val email: String,
        val password: String
    ) : AuthEvent()

    /** User chose to continue as guest */
    data object ContinueAsGuest : AuthEvent()

    data object SignOut : AuthEvent()
    data object ClearError : AuthEvent()
    data object DismissMigrationDialog : AuthEvent()
    data object RetryMigration : AuthEvent()
}
