package dev.panthu.mhikeapplication.presentation.hike

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.panthu.mhikeapplication.domain.model.AccessControl
import dev.panthu.mhikeapplication.domain.model.Hike
import dev.panthu.mhikeapplication.domain.model.ImageMetadata
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.usecase.DeleteImageUseCase
import dev.panthu.mhikeapplication.domain.usecase.SearchUsersUseCase
import dev.panthu.mhikeapplication.domain.usecase.UploadImageUseCase
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HikeViewModel @Inject constructor(
    private val repositoryProvider: dev.panthu.mhikeapplication.domain.provider.RepositoryProvider,
    private val authRepository: AuthRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    private val deleteImageUseCase: DeleteImageUseCase,
    private val searchUsersUseCase: SearchUsersUseCase
) : ViewModel() {

    // Get repository based on current auth state - synchronous version
    // For proper safety, use getHikeRepository() (suspend) in coroutines
    private val hikeRepositorySync: HikeRepository
        get() = repositoryProvider.getHikeRepositorySync()

    private val _uiState = MutableStateFlow(HikeUiState())
    val uiState: StateFlow<HikeUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(HikeFormState())
    val formState: StateFlow<HikeFormState> = _formState.asStateFlow()

    private var uploadJob: Job? = null
    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUserId = user?.uid
                if (user != null) {
                    loadHikes()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing upload jobs to prevent memory leaks
        uploadJob?.cancel()
    }

    fun onEvent(event: HikeEvent) {
        when (event) {
            // Form events
            is HikeEvent.NameChanged -> updateName(event.name)
            is HikeEvent.LocationChanged -> updateLocation(event.location)
            is HikeEvent.DateChanged -> updateDate(event.date)
            is HikeEvent.LengthChanged -> updateLength(event.length)
            is HikeEvent.DifficultyChanged -> updateDifficulty(event.difficulty)
            is HikeEvent.ParkingChanged -> updateParking(event.hasParking)
            is HikeEvent.DescriptionChanged -> updateDescription(event.description)

            // Image events
            is HikeEvent.ImageSelected -> uploadImage(event.uri)
            is HikeEvent.ImageDeleted -> deleteImage(event.image)
            is HikeEvent.CancelUpload -> cancelUpload()

            // User invitation events
            is HikeEvent.UserInvited -> inviteUser(event.user)
            is HikeEvent.UserUninvited -> uninviteUser(event.user)
            is HikeEvent.SearchUsers -> searchUsers(event.query)

            // CRUD events
            is HikeEvent.CreateHike -> createHike()
            is HikeEvent.LoadHike -> loadHike(event.hikeId)
            is HikeEvent.UpdateHike -> updateHike(event.hikeId)
            is HikeEvent.DeleteHike -> deleteHike(event.hikeId)

            // List events
            is HikeEvent.LoadHikes -> loadHikes()
            is HikeEvent.SearchHikes -> searchHikes(event.query)
            is HikeEvent.FilterByDifficulty -> filterByDifficulty(event.difficulty)
            is HikeEvent.FilterByLength -> filterByLength(event.min, event.max)
            is HikeEvent.ClearFilters -> clearFilters()

            // Sharing events
            is HikeEvent.ShareHike -> shareHike(event.hikeId, event.userId)
            is HikeEvent.RevokeAccess -> revokeAccess(event.hikeId, event.userId)

            // Navigation events
            is HikeEvent.NavigateBack -> { /* Handled by screen */ }
            is HikeEvent.ClearError -> clearError()
        }
    }

    private fun updateName(name: String) {
        _formState.update { state ->
            state.copy(
                name = name,
                nameError = when {
                    name.isBlank() -> "Name is required"
                    name.length < 3 -> "Name must be at least 3 characters"
                    name.length > 100 -> "Name must be less than 100 characters"
                    else -> null
                }
            )
        }
    }

    private fun updateLocation(location: dev.panthu.mhikeapplication.domain.model.Location) {
        _formState.update { state ->
            state.copy(
                location = location,
                locationError = when {
                    location.name.isBlank() -> "Location name is required"
                    location.coordinates.latitude == 0.0 && location.coordinates.longitude == 0.0 ->
                        "Please set a valid location"
                    else -> null
                }
            )
        }
    }

    private fun updateDate(date: Long) {
        _formState.update { state ->
            state.copy(
                date = date,
                dateError = if (date > System.currentTimeMillis()) {
                    "Date cannot be in the future"
                } else null
            )
        }
    }

    private fun updateLength(length: String) {
        _formState.update { state ->
            state.copy(
                length = length,
                lengthError = when {
                    length.isBlank() -> "Length is required"
                    length.toDoubleOrNull() == null -> "Length must be a valid number"
                    length.toDouble() <= 0 -> "Length must be greater than 0"
                    length.toDouble() > 1000 -> "Length seems unrealistic (max 1000 km)"
                    else -> null
                }
            )
        }
    }

    private fun updateDifficulty(difficulty: dev.panthu.mhikeapplication.domain.model.Difficulty) {
        _formState.update { it.copy(difficulty = difficulty) }
    }

    private fun updateParking(hasParking: Boolean) {
        _formState.update { it.copy(hasParking = hasParking) }
    }

    private fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    private fun uploadImage(uri: Uri) {
        val userId = currentUserId ?: return

        uploadJob?.cancel()

        // For creation, we'll use a temporary hikeId that will be replaced on save
        val tempHikeId = "temp_${System.currentTimeMillis()}"

        uploadJob = viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadError = null, uploadProgress = 0f) }

            uploadImageUseCase(uri, tempHikeId, null).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val progress = result.data
                        if (progress.isComplete) {
                            // Upload complete - add to form state
                            // Note: In real implementation, we'd get the download URL here
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    uploadProgress = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(uploadProgress = progress.progress)
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = null,
                                uploadError = result.message ?: "Upload failed"
                            )
                        }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isUploading = true) }
                    }
                }
            }
        }
    }

    private fun deleteImage(image: ImageMetadata) {
        viewModelScope.launch {
            when (val result = deleteImageUseCase(image.storagePath)) {
                is Result.Success -> {
                    _formState.update { state ->
                        state.copy(images = state.images.filter { it.id != image.id })
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = result.message ?: "Failed to delete image")
                    }
                }
                is Result.Loading -> { /* No action */ }
            }
        }
    }

    private fun cancelUpload() {
        uploadJob?.cancel()
        _uiState.update {
            it.copy(
                isUploading = false,
                uploadProgress = null
            )
        }
    }

    private fun inviteUser(user: dev.panthu.mhikeapplication.domain.model.User) {
        _formState.update { state ->
            if (user.uid !in state.invitedUsers.map { it.uid }) {
                state.copy(invitedUsers = state.invitedUsers + user)
            } else {
                state
            }
        }
    }

    private fun uninviteUser(user: dev.panthu.mhikeapplication.domain.model.User) {
        _formState.update { state ->
            state.copy(invitedUsers = state.invitedUsers.filter { it.uid != user.uid })
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true) }

            when (val result = searchUsersUseCase(query)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            searchResults = result.data,
                            isSearchingUsers = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            error = result.message ?: "User search failed",
                            isSearchingUsers = false
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isSearchingUsers = true) }
                }
            }
        }
    }

    private fun createHike() {
        val userId = currentUserId ?: return
        val form = _formState.value

        if (!form.isValid) {
            _uiState.update { it.copy(error = "Please fix form errors") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            val hike = Hike(
                id = "", // Firestore will generate
                ownerId = userId,
                name = form.name,
                location = form.location,
                date = Timestamp(Date(form.date)),
                length = form.length.toDouble(),
                difficulty = form.difficulty,
                hasParking = form.hasParking,
                description = form.description,
                imageUrls = form.images.map { it.url },
                accessControl = AccessControl(
                    invitedUsers = form.invitedUsers.map { it.uid }
                ),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.createHike(hike)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            currentHike = result.data
                        )
                    }
                    // Reset form
                    _formState.value = HikeFormState()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = result.message ?: "Failed to create hike"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun loadHike(hikeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.getHike(hikeId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentHike = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to load hike"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun updateHike(hikeId: String) {
        // Implementation for updating existing hike
        // Similar to createHike but uses hikeRepository.updateHike
    }

    private fun deleteHike(hikeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.deleteHike(hikeId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentHike = null
                        )
                    }
                    loadHikes() // Reload list
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to delete hike"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun loadHikes() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            repository.getAllHikes(userId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hikes = result.data
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load hikes"
                            )
                        }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun searchHikes(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // Filtering happens in the UI based on searchQuery
    }

    private fun filterByDifficulty(difficulty: dev.panthu.mhikeapplication.domain.model.Difficulty?) {
        _uiState.update { it.copy(filterDifficulty = difficulty) }
    }

    private fun filterByLength(min: Double?, max: Double?) {
        _uiState.update {
            it.copy(
                filterMinLength = min,
                filterMaxLength = max
            )
        }
    }

    private fun clearFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                filterDifficulty = null,
                filterMinLength = null,
                filterMaxLength = null
            )
        }
    }

    private fun shareHike(hikeId: String, userId: String) {
        viewModelScope.launch {
            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.shareHike(hikeId, userId)) {
                is Result.Success -> {
                    loadHike(hikeId) // Reload to get updated access control
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = result.message ?: "Failed to share hike")
                    }
                }
                is Result.Loading -> { /* No action */ }
            }
        }
    }

    private fun revokeAccess(hikeId: String, userId: String) {
        viewModelScope.launch {
            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.revokeAccess(hikeId, userId)) {
                is Result.Success -> {
                    loadHike(hikeId) // Reload to get updated access control
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = result.message ?: "Failed to revoke access")
                    }
                }
                is Result.Loading -> { /* No action */ }
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null, uploadError = null) }
    }

    fun resetForm() {
        _formState.value = HikeFormState()
        _uiState.update {
            it.copy(
                uploadProgress = null,
                isUploading = false,
                uploadError = null
            )
        }
    }
}
