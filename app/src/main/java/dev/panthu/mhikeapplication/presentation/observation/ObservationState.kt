package dev.panthu.mhikeapplication.presentation.observation

import com.google.firebase.firestore.GeoPoint
import dev.panthu.mhikeapplication.domain.model.ImageMetadata
import dev.panthu.mhikeapplication.domain.model.Location
import dev.panthu.mhikeapplication.domain.model.Observation

/**
 * UI state for observation management screens
 */
data class ObservationUiState(
    val isLoading: Boolean = false,
    val observations: List<Observation> = emptyList(),
    val currentObservation: Observation? = null,
    val error: String? = null,
    val isCreating: Boolean = false,
    val uploadProgress: Float? = null,
    val isUploading: Boolean = false,
    val uploadError: String? = null
)

/**
 * Form state for observation creation/editing
 */
data class ObservationFormState(
    val text: String = "",
    val location: Location? = null,
    val comments: String = "",
    val images: List<ImageMetadata> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),

    // Validation errors
    val textError: String? = null
) {
    val isValid: Boolean
        get() = text.isNotBlank() && textError == null
}

/**
 * Events for observation management
 */
sealed class ObservationEvent {
    // Form events
    data class TextChanged(val text: String) : ObservationEvent()
    data class LocationChanged(val location: Location) : ObservationEvent()
    data class CommentsChanged(val comments: String) : ObservationEvent()
    data class TimestampChanged(val timestamp: Long) : ObservationEvent()

    // Image events
    data class ImageSelected(val uri: android.net.Uri) : ObservationEvent()
    data class ImageDeleted(val image: ImageMetadata) : ObservationEvent()
    data object CancelUpload : ObservationEvent()

    // CRUD events
    data class CreateObservation(val hikeId: String) : ObservationEvent()
    data class LoadObservation(val hikeId: String, val observationId: String) : ObservationEvent()
    data class LoadObservations(val hikeId: String) : ObservationEvent()
    data class UpdateObservation(val hikeId: String, val observationId: String) : ObservationEvent()
    data class DeleteObservation(val hikeId: String, val observationId: String) : ObservationEvent()

    // Navigation events
    data object NavigateBack : ObservationEvent()
    data object ClearError : ObservationEvent()
}
