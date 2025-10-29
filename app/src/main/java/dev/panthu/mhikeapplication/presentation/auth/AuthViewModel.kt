package dev.panthu.mhikeapplication.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.domain.usecase.SignInUseCase
import dev.panthu.mhikeapplication.domain.usecase.SignOutUseCase
import dev.panthu.mhikeapplication.domain.usecase.SignUpUseCase
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(
                    isAuthenticated = user != null,
                    currentUser = user
                ) }
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
            is AuthEvent.SignOut -> signOut()
            is AuthEvent.ClearError -> clearError()
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

            when (val result = signUpUseCase(email, password, confirmPassword, displayName, handle)) {
                is Result.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.data
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Sign up failed"
                    ) }
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
                    _uiState.update { it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = result.data
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Sign in failed"
                    ) }
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
                    _uiState.update { it.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        currentUser = null
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Sign out failed"
                    ) }
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
}
