package dev.panthu.mhikeapplication.domain.usecase

import dev.panthu.mhikeapplication.domain.repository.ImageRepository
import dev.panthu.mhikeapplication.util.Result
import javax.inject.Inject

/**
 * Use case for deleting images from Firebase Storage
 */
class DeleteImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Delete a single image
     * @param storagePath Full storage path of the image
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(storagePath: String): Result<Unit> {
        if (storagePath.isBlank()) {
            return Result.Error(
                IllegalArgumentException("Storage path cannot be blank"),
                "Invalid storage path"
            )
        }

        return imageRepository.deleteImage(storagePath)
    }

    /**
     * Delete multiple images
     * @param storagePaths List of storage paths
     * @return Result indicating success or failure
     */
    suspend fun deleteMultiple(storagePaths: List<String>): Result<Unit> {
        if (storagePaths.isEmpty()) {
            return Result.Success(Unit)
        }

        val validPaths = storagePaths.filter { it.isNotBlank() }
        if (validPaths.isEmpty()) {
            return Result.Error(
                IllegalArgumentException("No valid storage paths provided"),
                "Invalid storage paths"
            )
        }

        return imageRepository.deleteImages(validPaths)
    }
}
