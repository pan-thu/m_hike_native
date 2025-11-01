package dev.panthu.mhikeapplication.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dev.panthu.mhikeapplication.domain.model.Observation
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
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
class ObservationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val networkManager: NetworkManager
) : ObservationRepository {

    private val hikesCollection = firestore.collection("hikes")

    override fun getObservationsForHike(hikeId: String): Flow<Result<List<Observation>>> = callbackFlow {
        trySend(Result.Loading)

        val listener = hikesCollection.document(hikeId)
            .collection("observations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val observations = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { Observation.fromMap(it) }
                    }
                    trySend(Result.Success(observations))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getObservation(
        hikeId: String,
        observationId: String
    ): Flow<Result<Observation?>> = callbackFlow {
        trySend(Result.Loading)

        val listener = hikesCollection.document(hikeId)
            .collection("observations")
            .document(observationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val observation = snapshot.data?.let { Observation.fromMap(it) }
                    trySend(Result.Success(observation))
                } else {
                    trySend(Result.Success(null))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createObservation(observation: Observation): Result<Observation> {
        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to create observations.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("createObservation")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            val now = Timestamp.now()
            val observationToCreate = observation.copy(
                createdAt = now,
                updatedAt = now
            )

            hikesCollection.document(observation.hikeId)
                .collection("observations")
                .document(observationToCreate.id)
                .set(observationToCreate.toMap())
                .await()

            observationToCreate
        }
    }

    override suspend fun updateObservation(observation: Observation): Result<Observation> {
        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to update observations.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("updateObservation")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            val now = Timestamp.now()
            val observationToUpdate = observation.copy(updatedAt = now)

            hikesCollection.document(observation.hikeId)
                .collection("observations")
                .document(observationToUpdate.id)
                .set(observationToUpdate.toMap())
                .await()

            observationToUpdate
        }
    }

    override suspend fun deleteObservation(
        hikeId: String,
        observationId: String
    ): Result<Unit> {
        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to delete observations.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("deleteObservation")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            hikesCollection.document(hikeId)
                .collection("observations")
                .document(observationId)
                .delete()
                .await()
        }
    }

    override suspend fun createObservations(observations: List<Observation>): Result<List<Observation>> {
        if (observations.isEmpty()) {
            return Result.Success(emptyList())
        }

        // Check authentication before Firestore operation
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error("Authentication required. Please sign in to create observations.")
        }

        // Check network before Firestore operation
        if (!networkManager.requireNetwork("createObservations")) {
            return Result.Error("No network connection. Please check your internet and try again.")
        }

        return safeCall {
            val now = Timestamp.now()
            val batch = firestore.batch()

            val observationsToCreate = observations.map { observation ->
                observation.copy(createdAt = now, updatedAt = now)
            }

            observationsToCreate.forEach { observation ->
                val docRef = hikesCollection.document(observation.hikeId)
                    .collection("observations")
                    .document(observation.id)
                batch.set(docRef, observation.toMap())
            }

            batch.commit().await()
            observationsToCreate
        }
    }
}
