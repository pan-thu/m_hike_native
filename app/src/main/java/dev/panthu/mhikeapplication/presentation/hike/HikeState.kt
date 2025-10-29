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
    val searchResults: List<User> = emptyList(),
    val isSearchingUsers: Boolean = false
)

/**
 * Form state for hike creation/editing
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
    val invitedUsers: List<User> = emptyList(),

    // Validation errors
    val nameError: String? = null,
    val locationError: String? = null,
    val lengthError: String? = null,
    val dateError: String? = null
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
                location.name.isNotBlank() &&
                length.toDoubleOrNull() != null &&
                length.toDouble() > 0 &&
                nameError == null &&
                locationError == null &&
                lengthError == null &&
                dateError == null
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

    // Image events
    data class ImageSelected(val uri: android.net.Uri) : HikeEvent()
    data class ImageDeleted(val image: ImageMetadata) : HikeEvent()
    data object CancelUpload : HikeEvent()

    // User invitation events
    data class UserInvited(val user: User) : HikeEvent()
    data class UserUninvited(val user: User) : HikeEvent()
    data class SearchUsers(val query: String) : HikeEvent()

    // CRUD events
    data object CreateHike : HikeEvent()
    data class LoadHike(val hikeId: String) : HikeEvent()
    data class UpdateHike(val hikeId: String) : HikeEvent()
    data class DeleteHike(val hikeId: String) : HikeEvent()

    // List events
    data object LoadHikes : HikeEvent()
    data class SearchHikes(val query: String) : HikeEvent()
    data class FilterByDifficulty(val difficulty: Difficulty?) : HikeEvent()
    data class FilterByLength(val min: Double?, val max: Double?) : HikeEvent()
    data object ClearFilters : HikeEvent()

    // Sharing events
    data class ShareHike(val hikeId: String, val userId: String) : HikeEvent()
    data class RevokeAccess(val hikeId: String, val userId: String) : HikeEvent()

    // Navigation events
    data object NavigateBack : HikeEvent()
    data object ClearError : HikeEvent()
}
