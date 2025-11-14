package dev.panthu.mhikeapplication.presentation.hike

import com.google.firebase.firestore.GeoPoint
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.domain.model.Hike
import dev.panthu.mhikeapplication.domain.model.ImageMetadata
import dev.panthu.mhikeapplication.domain.model.Location
import dev.panthu.mhikeapplication.domain.model.User

/**
 * UI state for hike management screens
 */
data class HikeUiState(
    val isLoading: Boolean = false,
    val hikes: List<Hike> = emptyList(),
    val allHikes: List<Hike> = emptyList(), // All hikes before filtering
    val currentHike: Hike? = null,
    val error: String? = null,
    val isCreating: Boolean = false,
    val uploadProgress: Float? = null,
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val searchQuery: String = "",
    val filterDifficulty: Difficulty? = null,
    val filterMinLength: Double? = null,
    val filterMaxLength: Double? = null,
    val filterHasParking: Boolean? = null,
    val filterLocation: String? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val searchResults: List<User> = emptyList(),
    val isSearchingUsers: Boolean = false
)

/**
 * Form state for hike creation/editing with comprehensive validation
 */
data class HikeFormState(
    val name: String = "",
    val location: Location = Location(
        name = "",
        coordinates = GeoPoint(0.0, 0.0),
        manualOverride = false
    ),
    val date: Long = System.currentTimeMillis(),
    val length: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val hasParking: Boolean = false,
    val description: String = "",
    val images: List<ImageMetadata> = emptyList(),
    val terrain: String = "",
    val groupSize: Int = 1,

    // Validation errors
    val nameError: String? = null,
    val locationError: String? = null,
    val lengthError: String? = null,
    val dateError: String? = null,
    val descriptionError: String? = null,
    val groupSizeError: String? = null
) {
    companion object {
        const val NAME_MIN_LENGTH = 3
        const val NAME_MAX_LENGTH = 100
        const val DESCRIPTION_MAX_LENGTH = 2000
        const val LENGTH_MAX_KM = 1000.0
        const val GROUP_SIZE_MIN = 1
        const val GROUP_SIZE_MAX = 100
        const val MAX_IMAGES = 1
        const val MAX_PAST_YEARS = 10
    }

    val isValid: Boolean
        get() = validateName() == null &&
                validateLocation() == null &&
                validateLength() == null &&
                validateDate() == null &&
                validateDescription() == null &&
                validateGroupSize() == null

    /**
     * Validate hike name
     * - Required
     * - 3-100 characters
     */
    fun validateName(): String? {
        return when {
            name.isBlank() -> "Hike name is required"
            name.length < NAME_MIN_LENGTH -> "Name must be at least $NAME_MIN_LENGTH characters"
            name.length > NAME_MAX_LENGTH -> "Name must be less than $NAME_MAX_LENGTH characters"
            else -> null
        }
    }

    /**
     * Validate location
     * - Required name
     * - Valid coordinates if provided
     */
    fun validateLocation(): String? {
        return when {
            location.name.isBlank() -> "Location name is required"
            location.coordinates != null && !isValidCoordinates(location.coordinates) ->
                "Invalid coordinates. Latitude must be -90 to 90, longitude -180 to 180"
            else -> null
        }
    }

    /**
     * Validate length
     * - Required
     * - Positive number
     * - Less than 1000 km
     */
    fun validateLength(): String? {
        val lengthValue = length.toDoubleOrNull()
        return when {
            length.isBlank() -> "Hike length is required"
            lengthValue == null -> "Length must be a valid number"
            lengthValue <= 0 -> "Length must be greater than 0"
            lengthValue > LENGTH_MAX_KM -> "Length must be less than $LENGTH_MAX_KM km"
            else -> null
        }
    }

    /**
     * Validate date
     * - No validation - allow any date (past or future)
     */
    fun validateDate(): String? {
        return null // No date validation
    }

    /**
     * Validate description
     * - Optional
     * - Max 2000 characters
     */
    fun validateDescription(): String? {
        return when {
            description.length > DESCRIPTION_MAX_LENGTH ->
                "Description must be less than $DESCRIPTION_MAX_LENGTH characters"
            else -> null
        }
    }

    /**
     * Validate group size
     * - 1-100 people
     */
    fun validateGroupSize(): String? {
        return when {
            groupSize < GROUP_SIZE_MIN -> "Group size must be at least $GROUP_SIZE_MIN"
            groupSize > GROUP_SIZE_MAX -> "Group size must be less than $GROUP_SIZE_MAX"
            else -> null
        }
    }

    /**
     * Validate images count
     */
    fun validateImages(): String? {
        return when {
            images.size > MAX_IMAGES -> "Only $MAX_IMAGES image allowed"
            else -> null
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
 * Events for hike management
 */
sealed class HikeEvent {
    // Form events
    data class NameChanged(val name: String) : HikeEvent()
    data class LocationChanged(val location: Location) : HikeEvent()
    data class DateChanged(val date: Long) : HikeEvent()
    data class LengthChanged(val length: String) : HikeEvent()
    data class DifficultyChanged(val difficulty: Difficulty) : HikeEvent()
    data class ParkingChanged(val hasParking: Boolean) : HikeEvent()
    data class DescriptionChanged(val description: String) : HikeEvent()
    data class GroupSizeChanged(val groupSize: String) : HikeEvent()

    // Image events
    data class ImageSelected(val uri: android.net.Uri) : HikeEvent()
    data class ImageDeleted(val image: ImageMetadata) : HikeEvent()
    data object CancelUpload : HikeEvent()

    // User search events
    data class SearchUsers(val query: String) : HikeEvent()

    // CRUD events
    data object CreateHike : HikeEvent()
    data class LoadHike(val hikeId: String) : HikeEvent()
    data class UpdateHike(val hikeId: String) : HikeEvent()
    data class DeleteHike(val hikeId: String) : HikeEvent()

    // List events
    data object LoadHikes : HikeEvent()
    data object LoadMyHikes : HikeEvent()
    data object LoadSharedHikes : HikeEvent()
    data class SearchHikes(val query: String) : HikeEvent()
    data class FilterByDifficulty(val difficulty: Difficulty?) : HikeEvent()
    data class FilterByLength(val min: Double?, val max: Double?) : HikeEvent()
    data class FilterByParking(val hasParking: Boolean?) : HikeEvent()
    data class AdvancedSearch(
        val name: String?,
        val location: String?,
        val minLength: Double?,
        val maxLength: Double?,
        val startDate: Long?,
        val endDate: Long?
    ) : HikeEvent()
    data object ClearFilters : HikeEvent()

    // Sharing events
    data class ShareHike(val hikeId: String, val userId: String) : HikeEvent()
    data class RevokeAccess(val hikeId: String, val userId: String) : HikeEvent()

    // Navigation events
    data object NavigateBack : HikeEvent()
    data object ClearError : HikeEvent()

    // Database management events
    data object ResetDatabase : HikeEvent()
}
