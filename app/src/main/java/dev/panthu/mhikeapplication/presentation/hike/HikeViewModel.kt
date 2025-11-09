package dev.panthu.mhikeapplication.presentation.hike

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.panthu.mhikeapplication.domain.model.AccessControl
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.domain.model.Hike
import dev.panthu.mhikeapplication.domain.model.ImageMetadata
import dev.panthu.mhikeapplication.domain.repository.AuthRepository
import dev.panthu.mhikeapplication.domain.repository.HikeRepository
import dev.panthu.mhikeapplication.domain.usecase.DeleteImageUseCase
import dev.panthu.mhikeapplication.domain.usecase.SearchUsersUseCase
import dev.panthu.mhikeapplication.domain.usecase.UploadImageUseCase
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HikeViewModel @Inject constructor(
    private val repositoryProvider: dev.panthu.mhikeapplication.domain.provider.RepositoryProvider,
    private val authRepository: AuthRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    private val deleteImageUseCase: DeleteImageUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val localImageRepository: dev.panthu.mhikeapplication.data.local.repository.LocalImageRepository,
    private val database: dev.panthu.mhikeapplication.data.local.MHikeDatabase,
    private val guestIdManager: dev.panthu.mhikeapplication.util.GuestIdManager
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
                android.util.Log.d("HikeViewModel", "Auth state changed. currentUserId set to: $currentUserId")
                // Don't automatically load hikes - let each screen decide what to load
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
            is HikeEvent.GroupSizeChanged -> updateGroupSize(event.groupSize)

            // Image events
            is HikeEvent.ImageSelected -> uploadImage(event.uri)
            is HikeEvent.ImageDeleted -> deleteImage(event.image)
            is HikeEvent.CancelUpload -> cancelUpload()

            // User search events
            is HikeEvent.SearchUsers -> searchUsers(event.query)

            // CRUD events
            is HikeEvent.CreateHike -> createHike()
            is HikeEvent.LoadHike -> loadHike(event.hikeId)
            is HikeEvent.UpdateHike -> updateHike(event.hikeId)
            is HikeEvent.DeleteHike -> deleteHike(event.hikeId)

            // List events
            is HikeEvent.LoadHikes -> loadHikes()
            is HikeEvent.LoadMyHikes -> loadMyHikes()
            is HikeEvent.LoadSharedHikes -> loadSharedHikes()
            is HikeEvent.SearchHikes -> searchHikes(event.query)
            is HikeEvent.FilterByDifficulty -> filterByDifficulty(event.difficulty)
            is HikeEvent.FilterByLength -> filterByLength(event.min, event.max)
            is HikeEvent.FilterByParking -> filterByParking(event.hasParking)
            is HikeEvent.ClearFilters -> clearFilters()

            // Sharing events
            is HikeEvent.ShareHike -> shareHike(event.hikeId, event.userId)
            is HikeEvent.RevokeAccess -> revokeAccess(event.hikeId, event.userId)

            // Navigation events
            is HikeEvent.NavigateBack -> { /* Handled by screen */ }
            is HikeEvent.ClearError -> clearError()

            // Database management events
            is HikeEvent.ResetDatabase -> resetDatabase()
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
                dateError = null // No date validation - allow both past and future dates
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

    private fun updateGroupSize(groupSize: String) {
        _formState.update { state ->
            val groupSizeValue = groupSize.toIntOrNull() ?: 0
            state.copy(
                groupSize = groupSizeValue,
                groupSizeError = when {
                    groupSize.isBlank() -> "Group size is required"
                    groupSize.toIntOrNull() == null -> "Group size must be a valid number"
                    groupSizeValue < HikeFormState.GROUP_SIZE_MIN -> "Group size must be at least ${HikeFormState.GROUP_SIZE_MIN}"
                    groupSizeValue > HikeFormState.GROUP_SIZE_MAX -> "Group size must be less than ${HikeFormState.GROUP_SIZE_MAX}"
                    else -> null
                }
            )
        }
    }

    private fun uploadImage(uri: Uri) {
        uploadJob?.cancel()

        uploadJob = viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadError = null, uploadProgress = 0f) }

            // Check if user is authenticated (guest mode vs authenticated mode)
            val isGuest = currentUserId == null

            if (isGuest) {
                // Guest mode: Save image locally
                val tempHikeId = "temp_${System.currentTimeMillis()}"
                when (val result = localImageRepository.saveImage(uri, "hikes/$tempHikeId")) {
                    is Result.Success -> {
                        val localPath = result.data
                        val imageMetadata = ImageMetadata(
                            id = java.util.UUID.randomUUID().toString(),
                            url = localPath,
                            thumbnailUrl = localPath,
                            storagePath = localPath,
                            contentType = "image/jpeg",
                            size = java.io.File(localPath).length(),
                            uploadedAt = com.google.firebase.Timestamp.now(),
                            uploadedBy = "guest"
                        )

                        // Add to form state
                        _formState.update { state ->
                            state.copy(images = state.images + imageMetadata)
                        }

                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = null,
                                uploadError = result.message ?: "Failed to save image"
                            )
                        }
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isUploading = true) }
                    }
                }
            } else {
                // Authenticated mode: Upload to Firebase
                val tempHikeId = "temp_${System.currentTimeMillis()}"

                uploadImageUseCase(uri, tempHikeId, null).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val progress = result.data
                            if (progress.isComplete) {
                                android.util.Log.d("HikeViewModel", "Upload complete! downloadUrl=${progress.downloadUrl}, storagePath=${progress.storagePath}")

                                // Upload complete - create ImageMetadata and add to form state
                                val imageMetadata = ImageMetadata(
                                    id = progress.imageId,
                                    url = progress.downloadUrl,
                                    thumbnailUrl = progress.downloadUrl,
                                    storagePath = progress.storagePath,
                                    contentType = "image/jpeg",
                                    size = progress.totalBytes,
                                    uploadedAt = com.google.firebase.Timestamp.now(),
                                    uploadedBy = currentUserId ?: ""
                                )

                                android.util.Log.d("HikeViewModel", "Adding image to form state: ${imageMetadata.url}")

                                _formState.update { state ->
                                    state.copy(images = state.images + imageMetadata)
                                }

                                android.util.Log.d("HikeViewModel", "Form state now has ${_formState.value.images.size} images")

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
    }

    private fun deleteImage(image: ImageMetadata) {
        viewModelScope.launch {
            // Check if user is in guest mode
            val isGuest = currentUserId == null

            if (isGuest) {
                // Guest mode: Just remove from form state without deleting file
                // The file will be cleaned up when the hike is updated/deleted
                _formState.update { state ->
                    state.copy(images = state.images.filter { it.id != image.id })
                }
            } else {
                // Authenticated mode: Delete from Firebase Storage
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
        val form = _formState.value

        if (!form.isValid) {
            _uiState.update { it.copy(error = "Please fix form errors") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            // Get the current user directly from auth repository to avoid race condition
            val userId = when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> result.data?.uid ?: "guest"
                else -> "guest"
            }

            // Debug: Log image metadata
            android.util.Log.d("HikeViewModel", "Creating hike with ${form.images.size} images")
            form.images.forEachIndexed { index, img ->
                android.util.Log.d("HikeViewModel", "Image $index: url=${img.url}, storagePath=${img.storagePath}")
            }

            val hike = Hike(
                id = java.util.UUID.randomUUID().toString(),
                ownerId = userId,
                name = form.name,
                location = form.location,
                date = Timestamp(Date(form.date)),
                length = form.length.toDouble(),
                difficulty = form.difficulty,
                hasParking = form.hasParking,
                description = form.description,
                groupSize = form.groupSize,
                imageUrls = form.images.map { it.url },
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            android.util.Log.d("HikeViewModel", "Hike imageUrls: ${hike.imageUrls}")

            // Get repository safely with mutex lock
            // Will return local repository for guest mode, remote for authenticated
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
            repository.getHike(hikeId).collect { result ->
                when (result) {
                    is Result.Success<*> -> {
                        val hike = result.data as? dev.panthu.mhikeapplication.domain.model.Hike
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentHike = hike
                            )
                        }

                        // Populate form state for editing
                        hike?.let { loadedHike ->
                            _formState.update {
                                HikeFormState(
                                    name = loadedHike.name,
                                    location = loadedHike.location,
                                    date = loadedHike.date.toDate().time,
                                    length = loadedHike.length.toString(),
                                    difficulty = loadedHike.difficulty,
                                    hasParking = loadedHike.hasParking,
                                    description = loadedHike.description,
                                    groupSize = loadedHike.groupSize,
                                    images = loadedHike.imageUrls.map { url ->
                                        ImageMetadata(
                                            id = "",
                                            url = url,
                                            thumbnailUrl = url,
                                            storagePath = url,
                                            contentType = "image/jpeg",
                                            size = 0,
                                            uploadedAt = loadedHike.createdAt,
                                            uploadedBy = loadedHike.ownerId
                                        )
                                    }
                                )
                            }
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
    }

    private fun updateHike(hikeId: String) {
        val form = _formState.value

        if (!form.isValid) {
            _uiState.update { it.copy(error = "Please fix form errors") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            // Get the current user directly from auth repository to avoid race condition
            val userId = when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> result.data?.uid ?: "guest"
                else -> "guest"
            }

            // Get the current hike to preserve fields not in the form
            val currentHike = _uiState.value.currentHike
            if (currentHike == null) {
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        error = "Hike not loaded"
                    )
                }
                return@launch
            }

            val updatedHike = currentHike.copy(
                name = form.name,
                location = form.location,
                date = Timestamp(Date(form.date)),
                length = form.length.toDouble(),
                difficulty = form.difficulty,
                hasParking = form.hasParking,
                description = form.description,
                groupSize = form.groupSize,
                imageUrls = form.images.map { it.url },
                updatedAt = Timestamp.now()
            )

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.updateHike(updatedHike)) {
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
                            error = result.message ?: "Failed to update hike"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun deleteHike(hikeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get the current user directly from auth repository to avoid race condition
            val userId = when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> result.data?.uid ?: "guest"
                else -> "guest"
            }

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            when (val result = repository.deleteHike(hikeId, userId)) {
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get the current user directly from auth repository to avoid race condition
            val userId = when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> result.data?.uid ?: "guest"
                else -> "guest"
            }

            // Get repository safely with mutex lock
            // Will return local repository for guest mode, remote for authenticated
            val repository = repositoryProvider.getHikeRepository()
            repository.getAllHikes(userId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allHikes = result.data,
                                hikes = applyFilters(result.data, it.searchQuery, it.filterDifficulty, it.filterMinLength, it.filterMaxLength, it.filterHasParking)
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

    private fun loadMyHikes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get the current user directly from auth repository to avoid race condition
            val userId = when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> result.data?.uid ?: "guest"
                else -> "guest"
            }

            android.util.Log.d("HikeViewModel", "loadMyHikes called with userId: $userId (currentUserId: $currentUserId)")

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            android.util.Log.d("HikeViewModel", "Repository obtained, calling getMyHikes")
            repository.getMyHikes(userId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        android.util.Log.d("HikeViewModel", "loadMyHikes received ${result.data.size} hikes")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allHikes = result.data,
                                hikes = applyFilters(result.data, it.searchQuery, it.filterDifficulty, it.filterMinLength, it.filterMaxLength, it.filterHasParking)
                            )
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e("HikeViewModel", "loadMyHikes error: ${result.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load hikes"
                            )
                        }
                    }
                    is Result.Loading -> {
                        android.util.Log.d("HikeViewModel", "loadMyHikes loading...")
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun loadSharedHikes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Get the current user directly from auth repository to avoid race condition
            val userId = when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> result.data?.uid ?: "guest"
                else -> "guest"
            }

            android.util.Log.d("HikeViewModel", "loadSharedHikes called with userId: $userId (currentUserId: $currentUserId)")

            // Get repository safely with mutex lock
            val repository = repositoryProvider.getHikeRepository()
            android.util.Log.d("HikeViewModel", "Repository obtained, calling getSharedHikes")
            repository.getSharedHikes(userId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        android.util.Log.d("HikeViewModel", "loadSharedHikes received ${result.data.size} hikes")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allHikes = result.data,
                                hikes = applyFilters(result.data, it.searchQuery, it.filterDifficulty, it.filterMinLength, it.filterMaxLength, it.filterHasParking)
                            )
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e("HikeViewModel", "loadSharedHikes error: ${result.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load shared hikes"
                            )
                        }
                    }
                    is Result.Loading -> {
                        android.util.Log.d("HikeViewModel", "loadSharedHikes loading...")
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun searchHikes(query: String) {
        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                searchQuery = query,
                hikes = applyFilters(currentState.allHikes, query, currentState.filterDifficulty, currentState.filterMinLength, currentState.filterMaxLength, currentState.filterHasParking)
            )
        }
    }

    private fun filterByDifficulty(difficulty: Difficulty?) {
        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                filterDifficulty = difficulty,
                hikes = applyFilters(currentState.allHikes, currentState.searchQuery, difficulty, currentState.filterMinLength, currentState.filterMaxLength, currentState.filterHasParking)
            )
        }
    }

    private fun filterByLength(min: Double?, max: Double?) {
        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                filterMinLength = min,
                filterMaxLength = max,
                hikes = applyFilters(currentState.allHikes, currentState.searchQuery, currentState.filterDifficulty, min, max, currentState.filterHasParking)
            )
        }
    }

    private fun filterByParking(hasParking: Boolean?) {
        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                filterHasParking = hasParking,
                hikes = applyFilters(currentState.allHikes, currentState.searchQuery, currentState.filterDifficulty, currentState.filterMinLength, currentState.filterMaxLength, hasParking)
            )
        }
    }

    private fun clearFilters() {
        val currentState = _uiState.value
        _uiState.update {
            it.copy(
                searchQuery = "",
                filterDifficulty = null,
                filterMinLength = null,
                filterMaxLength = null,
                filterHasParking = null,
                hikes = currentState.allHikes
            )
        }
    }

    private fun applyFilters(
        hikes: List<Hike>,
        searchQuery: String,
        difficulty: Difficulty?,
        minLength: Double?,
        maxLength: Double?,
        hasParking: Boolean?
    ): List<Hike> {
        return hikes.filter { hike ->
            // Search filter - match name, location, or description
            val matchesSearch = searchQuery.isBlank() ||
                    hike.name.contains(searchQuery, ignoreCase = true) ||
                    hike.location.name.contains(searchQuery, ignoreCase = true) ||
                    hike.description.contains(searchQuery, ignoreCase = true)

            // Difficulty filter
            val matchesDifficulty = difficulty == null || hike.difficulty == difficulty

            // Length filter
            val matchesMinLength = minLength == null || hike.length >= minLength
            val matchesMaxLength = maxLength == null || hike.length <= maxLength

            // Parking filter
            val matchesParking = hasParking == null || hike.hasParking == hasParking

            matchesSearch && matchesDifficulty && matchesMinLength && matchesMaxLength && matchesParking
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

    private fun resetDatabase() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Perform database operations on IO thread
                withContext(Dispatchers.IO) {
                    // Clear all database tables
                    database.clearAllTables()

                    // Reset guest ID and all SharedPreferences
                    guestIdManager.resetAll()
                }

                // Clear in-memory state (back on main thread)
                _uiState.update {
                    HikeUiState(isLoading = false)
                }
                _formState.value = HikeFormState()

                android.util.Log.d("HikeViewModel", "Database and guest data reset successfully")
            } catch (e: Exception) {
                android.util.Log.e("HikeViewModel", "Failed to reset database", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to reset database: ${e.message}"
                    )
                }
            }
        }
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
