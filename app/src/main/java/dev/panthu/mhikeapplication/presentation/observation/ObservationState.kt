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
 * Form state for observation creation/editing with comprehensive validation
 */
data class ObservationFormState(
    val text: String = "",
    val location: Location? = null,
    val comments: String = "",
    val images: List<ImageMetadata> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),

    // Validation errors
    val textError: String? = null,
    val commentsError: String? = null,
    val imagesError: String? = null
) {
    companion object {
        const val TEXT_MIN_LENGTH = 3
        const val TEXT_MAX_LENGTH = 500
        const val COMMENTS_MAX_LENGTH = 1000
        const val MAX_IMAGES = 10
        const val MAX_IMAGE_SIZE_MB = 5
    }

    val isValid: Boolean
        get() = validateText() == null &&
                validateComments() == null &&
                validateImages() == null

    /**
     * Validate observation text
     * - Required
     * - 3-500 characters
     */
    fun validateText(): String? {
        return when {
            text.isBlank() -> "Observation text is required"
            text.length < TEXT_MIN_LENGTH -> "Observation must be at least $TEXT_MIN_LENGTH characters"
            text.length > TEXT_MAX_LENGTH -> "Observation must be less than $TEXT_MAX_LENGTH characters"
            else -> null
        }
    }

    /**
     * Validate comments
     * - Optional
     * - Max 1000 characters
     */
    fun validateComments(): String? {
        return when {
            comments.length > COMMENTS_MAX_LENGTH ->
                "Comments must be less than $COMMENTS_MAX_LENGTH characters"
            else -> null
        }
    }

    /**
     * Validate images
     * - Max 10 images
     * - Each max 5MB (size validation would need to be done at upload time)
     */
    fun validateImages(): String? {
        return when {
            images.size > MAX_IMAGES -> "Maximum $MAX_IMAGES images allowed per observation"
            else -> null
        }
    }

    /**
     * Validate location coordinates if provided
     */
    fun validateLocation(): String? {
        return if (location?.coordinates != null && !isValidCoordinates(location.coordinates)) {
            "Invalid coordinates. Latitude must be -90 to 90, longitude -180 to 180"
        } else {
            null
        }
    }

    /**
     * Helper function to validate coordinates
     */
    private fun isValidCoordinates(coords: GeoPoint): Boolean {
        return coords.latitude in -90.0..90.0 &&
               coords.longitude in -180.0..180.0
    }
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
