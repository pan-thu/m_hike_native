package dev.panthu.mhikeapplication.domain.usecase

import android.net.Uri
import dev.panthu.mhikeapplication.domain.model.UploadProgress
import dev.panthu.mhikeapplication.domain.repository.ImageRepository
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for uploading images with validation and progress tracking
 */
class UploadImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Upload an image to Firebase Storage
     * @param uri Local URI of the image
     * @param hikeId ID of the hike
     * @param observationId Optional observation ID (null for hike images)
     * @return Flow emitting upload progress updates
     */
    operator fun invoke(
        uri: Uri,
        hikeId: String,
        observationId: String? = null
    ): Flow<Result<UploadProgress>> {
        // Validate inputs
        if (hikeId.isBlank()) {
            throw IllegalArgumentException("Hike ID cannot be blank")
        }

        // Repository handles validation and upload with progress
        return imageRepository.uploadImage(uri, hikeId, observationId)
    }
}
