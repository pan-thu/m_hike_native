package dev.panthu.mhikeapplication.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.util.Result
import dev.panthu.mhikeapplication.util.safeCall
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection = firestore.collection("users")

    override val currentUser: Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Fetch user from Firestore
                usersCollection.document(firebaseUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val user = document.data?.let { User.fromMap(it) }
                            trySend(user)
                        } else {
                            trySend(null)
                        }
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override val isAuthenticated: Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        handle: String
    ): Result<User> = safeCall {
        // Check if handle is already taken
        val handleQuery = usersCollection.whereEqualTo("handle", handle).get().await()
        if (!handleQuery.isEmpty) {
            throw Exception("Handle already taken")
        }

        // Create Firebase Auth user
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("Failed to create user")

        // Update Firebase Auth profile
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        firebaseUser.updateProfile(profileUpdates).await()

        // Create Firestore user document
        val now = Timestamp.now()
        val user = User(
            uid = firebaseUser.uid,
            email = email,
            displayName = displayName,
            handle = handle,
            createdAt = now,
            updatedAt = now,
            isActive = true,
            lastLogin = now
        )

        usersCollection.document(firebaseUser.uid).set(user.toMap()).await()
        user
    }

    override suspend fun signIn(email: String, password: String): Result<User> = safeCall {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("Failed to sign in")

        // Get user from Firestore
        val userDoc = usersCollection.document(firebaseUser.uid).get().await()
        if (!userDoc.exists()) {
            throw Exception("User not found in database")
        }

        val user = userDoc.data?.let { User.fromMap(it) } ?: throw Exception("Invalid user data")

        // Check if user is active
        if (!user.isActive) {
            auth.signOut()
            throw Exception("Account is deactivated. Please contact support to reactivate.")
        }

        // Update last login timestamp
        usersCollection.document(firebaseUser.uid).update(
            mapOf(
                "metadata.lastLogin" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()

        user
    }

    override suspend fun signOut(): Result<Unit> = safeCall {
        auth.signOut()
    }

    override suspend fun deactivateAccount(): Result<Unit> = safeCall {
        val userId = auth.currentUser?.uid ?: throw Exception("No user signed in")

        usersCollection.document(userId).update(
            mapOf(
                "isActive" to false,
                "updatedAt" to Timestamp.now()
            )
        ).await()

        auth.signOut()
    }

    override suspend fun reactivateAccount(): Result<Unit> = safeCall {
        val userId = auth.currentUser?.uid ?: throw Exception("No user signed in")

        usersCollection.document(userId).update(
            mapOf(
                "isActive" to true,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    override suspend fun getCurrentUser(): Result<User?> = safeCall {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            if (userDoc.exists()) {
                userDoc.data?.let { User.fromMap(it) }
            } else {
                null
            }
        } else {
            null
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = safeCall {
        val results = mutableListOf<User>()

        // Search by email (exact match)
        // Remove isActive filter from query to avoid composite index requirement
        val emailQuery = usersCollection
            .whereEqualTo("email", query)
            .get()
            .await()
        results.addAll(emailQuery.documents.mapNotNull { it.data?.let { data -> User.fromMap(data) } })

        // Search by handle (exact match)
        // Remove isActive filter from query to avoid composite index requirement
        val handleQuery = usersCollection
            .whereEqualTo("handle", query)
            .get()
            .await()
        results.addAll(handleQuery.documents.mapNotNull { it.data?.let { data -> User.fromMap(data) } })

        // Search by displayName (prefix match using >= and <)
        val displayNameQuery = usersCollection
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThan("displayName", query + '\uf8ff')
            .get()
            .await()
        results.addAll(displayNameQuery.documents.mapNotNull { it.data?.let { data -> User.fromMap(data) } })

        // Remove duplicates by uid and filter out inactive users in memory
        results.distinctBy { it.uid }.filter { it.isActive }
    }
}
