package dev.panthu.mhikeapplication.domain.repository

import android.net.Uri
import dev.panthu.mhikeapplication.domain.model.ImageMetadata
import dev.panthu.mhikeapplication.domain.model.UploadProgress
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    /**
     * Upload an image to Firebase Storage with progress tracking
     * @param uri Local URI of the image to upload
     * @param hikeId ID of the hike this image belongs to
     * @param observationId Optional ID of the observation (null for hike images)
     * @return Flow emitting upload progress updates
     */
    fun uploadImage(
        uri: Uri,
        hikeId: String,
        observationId: String? = null
    ): Flow<Result<UploadProgress>>

    /**
     * Delete an image from Firebase Storage
     * @param storagePath Full storage path of the image
     * @return Result indicating success or failure
     */
    suspend fun deleteImage(storagePath: String): Result<Unit>

    /**
     * Delete multiple images from Firebase Storage
     * @param storagePaths List of storage paths to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteImages(storagePaths: List<String>): Result<Unit>

    /**
     * Get download URL for an image
     * @param storagePath Storage path of the image
     * @return Result containing the download URL
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String>

    /**
     * Generate thumbnail for an image
     * @param originalUri URI of the original image
     * @param maxSize Maximum dimension (width or height) for the thumbnail
     * @return Result containing URI of the generated thumbnail
     */
    suspend fun generateThumbnail(originalUri: Uri, maxSize: Int = 200): Result<Uri>

    /**
     * Validate image file
     * @param uri URI of the image to validate
     * @return Result indicating if image is valid, with error message if not
     */
    suspend fun validateImage(uri: Uri): Result<Unit>
}
