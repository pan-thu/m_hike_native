package dev.panthu.mhikeapplication.domain.usecase

import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.util.Result
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(query: String): Result<List<User>> {
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        if (query.length < 2) {
            return Result.Error(Exception("Search query must be at least 2 characters"))
        }
        return authRepository.searchUsers(query)
    }
}
