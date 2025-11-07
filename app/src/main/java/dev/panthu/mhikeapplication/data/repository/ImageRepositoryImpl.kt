package dev.panthu.mhikeapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.panthu.mhikeapplication.domain.model.UploadProgress
import dev.panthu.mhikeapplication.domain.repository.ImageRepository
import dev.panthu.mhikeapplication.util.Result
import dev.panthu.mhikeapplication.util.safeCall
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : ImageRepository {

    companion object {
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val THUMBNAIL_SIZE = 200
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
        )
    }

    override fun uploadImage(
        uri: Uri,
        hikeId: String,
        observationId: String?
    ): Flow<Result<UploadProgress>> = callbackFlow {
        try {
            // Validate image first
            val validationResult = validateImage(uri)
            if (validationResult is Result.Error) {
                trySend(validationResult)
                close()
                return@callbackFlow
            }

            val userId = auth.currentUser?.uid
                ?: throw SecurityException("User not authenticated")

            // Generate unique image ID
            val imageId = UUID.randomUUID().toString()

            // Determine storage path
            val storagePath = if (observationId != null) {
                "hikes/$hikeId/observations/$observationId/images/$imageId"
            } else {
                "hikes/$hikeId/images/$imageId"
            }

            // Get file extension from URI
            val extension = getFileExtension(uri) ?: "jpg"
            val fullPath = "$storagePath.$extension"

            // Create storage reference
            val storageRef = storage.reference.child(fullPath)

            // Get content type
            val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"

            // Create metadata
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .setCustomMetadata("uploadedBy", userId)
                .setCustomMetadata("hikeId", hikeId)
                .apply {
                    observationId?.let { setCustomMetadata("observationId", it) }
                }
                .build()

            // Start upload
            val uploadTask = storageRef.putFile(uri, metadata)

            // Track progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = UploadProgress(
                    imageId = imageId,
                    bytesTransferred = taskSnapshot.bytesTransferred,
                    totalBytes = taskSnapshot.totalByteCount,
                    isComplete = false
                )
                trySend(Result.Success(progress))
            }.addOnSuccessListener {
                val progress = UploadProgress(
                    imageId = imageId,
                    bytesTransferred = it.totalByteCount,
                    totalBytes = it.totalByteCount,
                    isComplete = true
                )
                trySend(Result.Success(progress))
                close()
            }.addOnFailureListener { exception ->
                val progress = UploadProgress(
                    imageId = imageId,
                    bytesTransferred = 0,
                    totalBytes = 0,
                    isComplete = false,
                    error = exception as? Exception ?: Exception(exception.message)
                )
                trySend(Result.Error(
                    exception as? Exception ?: Exception(exception.message),
                    "Upload failed: ${exception.message}"
                ))
                close()
            }

            awaitClose {
                // Cancel upload if flow is cancelled
                if (!uploadTask.isComplete) {
                    uploadTask.cancel()
                }
            }
        } catch (e: Exception) {
            trySend(Result.Error(e, "Failed to start upload: ${e.message}"))
            close()
        }
    }

    override suspend fun deleteImage(storagePath: String): Result<Unit> = safeCall {
        val storageRef = storage.reference.child(storagePath)
        storageRef.delete().await()

        // Try to delete thumbnail as well (non-blocking failure)
        try {
            val thumbnailPath = getThumbnailPath(storagePath)
            storage.reference.child(thumbnailPath).delete().await()
        } catch (e: Exception) {
            // Thumbnail deletion failure is non-critical
        }
    }

    override suspend fun deleteImages(storagePaths: List<String>): Result<Unit> = safeCall {
        storagePaths.forEach { path ->
            deleteImage(path)
        }
    }

    override suspend fun getDownloadUrl(storagePath: String): Result<String> = safeCall {
        val storageRef = storage.reference.child(storagePath)
        storageRef.downloadUrl.await().toString()
    }

    override suspend fun generateThumbnail(
        originalUri: Uri,
        maxSize: Int
    ): Result<Uri> = safeCall {
        // Read original image
        val inputStream = context.contentResolver.openInputStream(originalUri)
            ?: throw Exception("Cannot open image")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("Cannot decode image")

        inputStream.close()

        // Calculate new dimensions
        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val (width, height) = if (ratio > 1) {
            maxSize to (maxSize / ratio).toInt()
        } else {
            (maxSize * ratio).toInt() to maxSize
        }

        // Create thumbnail
        val thumbnail = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

        // Save to cache directory
        val cacheDir = File(context.cacheDir, "thumbnails")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val thumbnailFile = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
        FileOutputStream(thumbnailFile).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        // Clean up bitmaps
        originalBitmap.recycle()
        thumbnail.recycle()

        // Return URI
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            thumbnailFile
        )
    }

    override suspend fun validateImage(uri: Uri): Result<Unit> = safeCall {
        // Check if file exists
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot access image file")

        try {
            // Check file size
            val fileSize = inputStream.available().toLong()
            if (fileSize > MAX_IMAGE_SIZE) {
                throw Exception("Image size exceeds maximum allowed size of 10MB")
            }

            // Check mime type
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType !in ALLOWED_MIME_TYPES) {
                throw Exception("Image type not supported. Please use JPEG, PNG, or WebP")
            }

            // Verify image can be decoded
            val bitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw Exception("Invalid image file")

            bitmap.recycle()
        } finally {
            inputStream.close()
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> null
        }
    }

    private fun getThumbnailPath(originalPath: String): String {
        val lastDotIndex = originalPath.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            val pathWithoutExt = originalPath.substring(0, lastDotIndex)
            val extension = originalPath.substring(lastDotIndex)
            "${pathWithoutExt}_thumb$extension"
        } else {
            "${originalPath}_thumb"
        }
    }
}
