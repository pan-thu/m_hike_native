package dev.panthu.mhikeapplication.presentation.observation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.panthu.mhikeapplication.domain.model.Observation
import dev.panthu.mhikeapplication.domain.repository.ObservationRepository
import dev.panthu.mhikeapplication.domain.usecase.DeleteImageUseCase
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
class ObservationViewModel @Inject constructor(
    private val observationRepository: ObservationRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    private val deleteImageUseCase: DeleteImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObservationUiState())
    val uiState: StateFlow<ObservationUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ObservationFormState())
    val formState: StateFlow<ObservationFormState> = _formState.asStateFlow()

    private var uploadJob: Job? = null

    fun onEvent(event: ObservationEvent) {
        when (event) {
            // Form events
            is ObservationEvent.TextChanged -> updateText(event.text)
            is ObservationEvent.LocationChanged -> updateLocation(event.location)
            is ObservationEvent.CommentsChanged -> updateComments(event.comments)
            is ObservationEvent.TimestampChanged -> updateTimestamp(event.timestamp)

            // Image events
            is ObservationEvent.ImageSelected -> uploadImage(event.uri)
            is ObservationEvent.ImageDeleted -> deleteImage(event.image)
            is ObservationEvent.CancelUpload -> cancelUpload()

            // CRUD events
            is ObservationEvent.CreateObservation -> createObservation(event.hikeId)
            is ObservationEvent.LoadObservation -> loadObservation(event.hikeId, event.observationId)
            is ObservationEvent.LoadObservations -> loadObservations(event.hikeId)
            is ObservationEvent.UpdateObservation -> updateObservation(event.hikeId, event.observationId)
            is ObservationEvent.DeleteObservation -> deleteObservation(event.hikeId, event.observationId)

            // Navigation events
            is ObservationEvent.NavigateBack -> { /* Handled by screen */ }
            is ObservationEvent.ClearError -> clearError()
        }
    }

    private fun updateText(text: String) {
        _formState.update { state ->
            state.copy(
                text = text,
                textError = when {
                    text.isBlank() -> "Observation text is required"
                    text.length < 3 -> "Observation must be at least 3 characters"
                    text.length > 500 -> "Observation must be less than 500 characters"
                    else -> null
                }
            )
        }
    }

    private fun updateLocation(location: dev.panthu.mhikeapplication.domain.model.Location) {
        _formState.update { it.copy(location = location) }
    }

    private fun updateComments(comments: String) {
        _formState.update { it.copy(comments = comments) }
    }

    private fun updateTimestamp(timestamp: Long) {
        _formState.update { it.copy(timestamp = timestamp) }
    }

    private fun uploadImage(uri: Uri) {
        // For now, we'll handle image upload after observation creation
        // In production, you'd upload to temp storage first
        uploadJob?.cancel()

        val tempObservationId = "temp_${System.currentTimeMillis()}"
        val tempHikeId = "temp"

        uploadJob = viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadError = null, uploadProgress = 0f) }

            uploadImageUseCase(uri, tempHikeId, tempObservationId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val progress = result.data
                        if (progress.isComplete) {
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

    private fun deleteImage(image: dev.panthu.mhikeapplication.domain.model.ImageMetadata) {
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

    private fun createObservation(hikeId: String) {
        val form = _formState.value

        if (!form.isValid) {
            _uiState.update { it.copy(error = "Please fix form errors") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            val observation = Observation(
                id = "", // Firestore will generate
                hikeId = hikeId,
                text = form.text,
                timestamp = Timestamp(Date(form.timestamp)),
                location = form.location?.coordinates,
                imageUrls = form.images.map { it.url },
                comments = form.comments
            )

            when (val result = observationRepository.createObservation(observation)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            currentObservation = result.data
                        )
                    }
                    // Reset form
                    _formState.value = ObservationFormState()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = result.message ?: "Failed to create observation"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun loadObservation(hikeId: String, observationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = observationRepository.getObservation(hikeId, observationId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentObservation = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to load observation"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun loadObservations(hikeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            observationRepository.getObservationsForHike(hikeId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                observations = result.data
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load observations"
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

    private fun updateObservation(hikeId: String, observationId: String) {
        val form = _formState.value

        if (!form.isValid) {
            _uiState.update { it.copy(error = "Please fix form errors") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            val observation = Observation(
                id = observationId,
                hikeId = hikeId,
                text = form.text,
                timestamp = Timestamp(Date(form.timestamp)),
                location = form.location?.coordinates,
                imageUrls = form.images.map { it.url },
                comments = form.comments,
                createdAt = _uiState.value.currentObservation?.createdAt ?: Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            when (val result = observationRepository.updateObservation(observation)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            currentObservation = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = result.message ?: "Failed to update observation"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun deleteObservation(hikeId: String, observationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = observationRepository.deleteObservation(hikeId, observationId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentObservation = null
                        )
                    }
                    loadObservations(hikeId) // Reload list
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to delete observation"
                        )
                    }
                }
                is Result.Loading -> { /* Already handled */ }
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null, uploadError = null) }
    }

    fun resetForm() {
        _formState.value = ObservationFormState()
        _uiState.update {
            it.copy(
                uploadProgress = null,
                isUploading = false,
                uploadError = null
            )
        }
    }
}
