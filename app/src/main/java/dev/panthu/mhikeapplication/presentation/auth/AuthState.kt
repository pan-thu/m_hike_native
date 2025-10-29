package dev.panthu.mhikeapplication.presentation.auth

import dev.panthu.mhikeapplication.domain.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)

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

    data object SignOut : AuthEvent()
    data object ClearError : AuthEvent()
}
