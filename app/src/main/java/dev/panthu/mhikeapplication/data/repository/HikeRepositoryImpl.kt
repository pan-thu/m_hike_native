package dev.panthu.mhikeapplication.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.domain.model.Hike
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.util.NetworkManager
import dev.panthu.mhikeapplication.util.Result
import dev.panthu.mhikeapplication.util.safeCall
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HikeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val networkManager: NetworkManager
) : HikeRepository {

    private val hikesCollection = firestore.collection("hikes")

    override fun getAllHikes(userId: String): Flow<Result<List<Hike>>> = callbackFlow {
        trySend(Result.Loading)

        val listener = hikesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val hikes = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Hike.fromMap(it) }
                    }.filter { hike ->
                        hike.hasReadAccess(userId)
                    }
                    trySend(Result.Success(hikes))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getHike(hikeId: String): Flow<Result<Hike?>> = callbackFlow {
        trySend(Result.Loading)

        val listener = hikesCollection.document(hikeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val hike = snapshot.data?.let { Hike.fromMap(it) }
                    trySend(Result.Success(hike))
                } else {
                    trySend(Result.Success(null))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getMyHikes(userId: String): Flow<Result<List<Hike>>> = callbackFlow {
        trySend(Result.Loading)

        val listener = hikesCollection
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val hikes = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Hike.fromMap(it) }
                    }
                    trySend(Result.Success(hikes))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getSharedHikes(userId: String): Flow<Result<List<Hike>>> = callbackFlow {
        trySend(Result.Loading)

        val listener = hikesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val hikes = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Hike.fromMap(it) }
                    }.filter { hike ->
                        hike.accessControl.hasAccess(userId) && !hike.isOwner(userId)
                    }
                    trySend(Result.Success(hikes))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createHike(hike: Hike): Result<Hike> {
        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to create hikes.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("createHike")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            val now = Timestamp.now()
            val hikeToCreate = hike.copy(
                createdAt = now,
                updatedAt = now
            )

            hikesCollection.document(hikeToCreate.id)
                .set(hikeToCreate.toMap())
                .await()

            hikeToCreate
        }
    }

    override suspend fun updateHike(hike: Hike): Result<Hike> {
        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to update hikes.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("updateHike")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            val now = Timestamp.now()
            val hikeToUpdate = hike.copy(updatedAt = now)

            hikesCollection.document(hikeToUpdate.id)
                .set(hikeToUpdate.toMap())
                .await()

            hikeToUpdate
        }
    }

    override suspend fun deleteHike(hikeId: String, userId: String): Result<Unit> {
        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to delete hikes.")
        }

        // Verify userId matches authenticated user
        if (currentUser.uid != userId) {
            return Result.Error("Authentication mismatch. Please sign in with the correct account.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("deleteHike")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            // Verify ownership before deletion
            val hikeDoc = hikesCollection.document(hikeId).get().await()
            if (!hikeDoc.exists()) {
                throw Exception("Hike not found")
            }

            val hike = hikeDoc.data?.let { Hike.fromMap(it) }
            if (hike?.ownerId != userId) {
                throw Exception("Only the owner can delete this hike")
            }

            // Delete all observations first
            val observations = hikesCollection.document(hikeId)
                .collection("observations")
                .get()
                .await()

            val batch = firestore.batch()
            observations.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            // Delete the hike
            batch.delete(hikesCollection.document(hikeId))
            batch.commit().await()
        }
    }

    override suspend fun addInvitedUsers(hikeId: String, userIds: List<String>): Result<Unit> = safeCall {
        val hikeDoc = hikesCollection.document(hikeId).get().await()
        if (!hikeDoc.exists()) {
            throw Exception("Hike not found")
        }

        val hike = hikeDoc.data?.let { Hike.fromMap(it) } ?: throw Exception("Invalid hike data")
        val updatedAccessControl = userIds.fold(hike.accessControl) { acc, userId ->
            acc.addInvitedUser(userId)
        }

        hikesCollection.document(hikeId).update(
            mapOf(
                "accessControl" to updatedAccessControl.toMap(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    override suspend fun shareHike(hikeId: String, userId: String): Result<Unit> = safeCall {
        val hikeDoc = hikesCollection.document(hikeId).get().await()
        if (!hikeDoc.exists()) {
            throw Exception("Hike not found")
        }

        val hike = hikeDoc.data?.let { Hike.fromMap(it) } ?: throw Exception("Invalid hike data")
        val updatedAccessControl = hike.accessControl.addSharedUser(userId)

        hikesCollection.document(hikeId).update(
            mapOf(
                "accessControl" to updatedAccessControl.toMap(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    override suspend fun revokeAccess(hikeId: String, userId: String): Result<Unit> = safeCall {
        val hikeDoc = hikesCollection.document(hikeId).get().await()
        if (!hikeDoc.exists()) {
            throw Exception("Hike not found")
        }

        val hike = hikeDoc.data?.let { Hike.fromMap(it) } ?: throw Exception("Invalid hike data")
        val updatedAccessControl = hike.accessControl.removeUser(userId)

        hikesCollection.document(hikeId).update(
            mapOf(
                "accessControl" to updatedAccessControl.toMap(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    override suspend fun searchHikes(query: String, userId: String): Result<List<Hike>> = safeCall {
        if (query.isBlank()) {
            return@safeCall emptyList()
        }

        val results = hikesCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThan("name", query + '\uf8ff')
            .get()
            .await()

        results.documents.mapNotNull { doc ->
            doc.data?.let { Hike.fromMap(it) }
        }.filter { hike ->
            hike.hasReadAccess(userId)
        }
    }

    override suspend fun filterHikes(
        userId: String,
        nameQuery: String?,
        locationQuery: String?,
        minLength: Double?,
        maxLength: Double?,
        startDate: Timestamp?,
        endDate: Timestamp?,
        difficulty: Difficulty?
    ): Result<List<Hike>> = safeCall {
        // Fetch all accessible hikes
        val allHikes = hikesCollection.get().await()
            .documents.mapNotNull { doc ->
                doc.data?.let { Hike.fromMap(it) }
            }.filter { hike ->
                hike.hasReadAccess(userId)
            }

        // Apply filters in memory (Firestore has limitations on compound queries)
        allHikes.filter { hike ->
            var matches = true

            if (nameQuery != null && nameQuery.isNotBlank()) {
                matches = matches && hike.name.contains(nameQuery, ignoreCase = true)
            }

            if (locationQuery != null && locationQuery.isNotBlank()) {
                matches = matches && hike.location.name.contains(locationQuery, ignoreCase = true)
            }

            if (minLength != null) {
                matches = matches && hike.length >= minLength
            }

            if (maxLength != null) {
                matches = matches && hike.length <= maxLength
            }

            if (startDate != null) {
                matches = matches && hike.date >= startDate
            }

            if (endDate != null) {
                matches = matches && hike.date <= endDate
            }

            if (difficulty != null) {
                matches = matches && hike.difficulty == difficulty
            }

            matches
        }
    }
}
