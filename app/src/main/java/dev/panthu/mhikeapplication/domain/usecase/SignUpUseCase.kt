package dev.panthu.mhikeapplication.domain.usecase

import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.util.Result
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String,
        handle: String
    ): Result<User> {
        // Validation
        if (email.isBlank()) {
            return Result.Error(Exception("Email cannot be empty"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.Error(Exception("Invalid email format"))
        }
        if (password.isBlank()) {
            return Result.Error(Exception("Password cannot be empty"))
        }
        if (password.length < 6) {
            return Result.Error(Exception("Password must be at least 6 characters"))
        }
        if (password != confirmPassword) {
            return Result.Error(Exception("Passwords do not match"))
        }
        if (displayName.isBlank()) {
            return Result.Error(Exception("Display name cannot be empty"))
        }
        if (handle.isBlank()) {
            return Result.Error(Exception("Handle cannot be empty"))
        }
        if (handle.length < 3) {
            return Result.Error(Exception("Handle must be at least 3 characters"))
        }
        if (!handle.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return Result.Error(Exception("Handle can only contain letters, numbers, and underscores"))
        }

        return authRepository.signUp(email, password, displayName, handle)
    }
}
