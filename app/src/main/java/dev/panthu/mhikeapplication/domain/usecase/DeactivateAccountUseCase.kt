package dev.panthu.mhikeapplication.domain.usecase

import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.util.Result
import javax.inject.Inject

class DeactivateAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.deactivateAccount()
    }
}
