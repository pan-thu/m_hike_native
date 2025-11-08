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

        android.util.Log.d("HikeRepository", "getMyHikes called with userId: $userId")

        val listener = hikesCollection
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("HikeRepository", "Error loading my hikes: ${error.message}")
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val hikes = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Hike.fromMap(it) }
                    }
                    android.util.Log.d("HikeRepository", "getMyHikes returned ${hikes.size} hikes")
                    trySend(Result.Success(hikes))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getSharedHikes(userId: String): Flow<Result<List<Hike>>> = callbackFlow {
        trySend(Result.Loading)

        android.util.Log.d("HikeRepository", "getSharedHikes called with userId: $userId")

        // For now, load all hikes and filter in memory to support both old and new formats
        // This ensures we catch hikes with either structure
        val listener = hikesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sharedHikes = mutableListOf<Hike>()

                    android.util.Log.d("HikeRepository", "Total hikes in Firestore: ${snapshot.documents.size}")

                    snapshot.documents.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val hike = Hike.fromMap(data)

                        android.util.Log.d("HikeRepository", "Checking hike ${hike.id}, owner: ${hike.ownerId}, sharedWith: ${hike.accessControl.sharedWith}")

                        // Skip if user owns this hike
                        if (hike.isOwner(userId)) {
                            android.util.Log.d("HikeRepository", "Skipping ${hike.id} - user is owner")
                            return@forEach
                        }

                        // Check new format
                        if (hike.accessControl.sharedWith.contains(userId)) {
                            android.util.Log.d("HikeRepository", "Found shared hike (new format): ${hike.id}")
                            sharedHikes.add(hike)
                            return@forEach
                        }

                        // Check old format (in case Firestore still has old structure)
                        val accessData = data["accessControl"] as? Map<String, Any>
                        if (accessData != null) {
                            val invitedUsers = (accessData["invitedUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val sharedUsers = (accessData["sharedUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            if (userId in invitedUsers || userId in sharedUsers) {
                                android.util.Log.d("HikeRepository", "Found shared hike (old format): ${hike.id}")
                                sharedHikes.add(hike)
                            }
                        }
                    }

                    android.util.Log.d("HikeRepository", "Returning ${sharedHikes.size} shared hikes")
                    trySend(Result.Success(sharedHikes))
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

    override suspend fun shareHike(hikeId: String, userId: String): Result<Unit> = safeCall {
        val hikeDoc = hikesCollection.document(hikeId).get().await()
        if (!hikeDoc.exists()) {
            throw Exception("Hike not found")
        }

        val hike = hikeDoc.data?.let { Hike.fromMap(it) } ?: throw Exception("Invalid hike data")
        val updatedAccessControl = hike.accessControl.addUser(userId)

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
