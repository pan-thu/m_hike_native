package dev.panthu.mhikeapplication.domain.repository

import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isAuthenticated: Flow<Boolean>

    suspend fun signUp(email: String, password: String, displayName: String, handle: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun deactivateAccount(): Result<Unit>
    suspend fun reactivateAccount(): Result<Unit>
    suspend fun getCurrentUser(): Result<User?>
    suspend fun searchUsers(query: String): Result<List<User>>
}
