package dev.panthu.mhikeapplication.domain.usecase

import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.util.Result
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
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

        return authRepository.signIn(email, password)
    }
}
