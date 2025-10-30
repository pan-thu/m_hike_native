package dev.panthu.mhikeapplication.data.local.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for local image storage in guest mode
 * Stores images in app's internal storage directory
 */
@Singleton
class LocalImageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imagesDir = File(context.filesDir, "images")

    init {
        // Ensure images directory exists
        imagesDir.mkdirs()
    }

    /**
     * Save an image from URI to local storage
     * @param imageUri Source URI (from gallery/camera)
     * @param targetDirectory Subdirectory (e.g., "hikes/{hikeId}")
     * @return Absolute path to saved image file
     */
    suspend fun saveImage(imageUri: Uri, targetDirectory: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val targetDir = File(imagesDir, targetDirectory)
                targetDir.mkdirs()

                val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val targetFile = File(targetDir, fileName)

                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Result.Success(targetFile.absolutePath)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to save image locally")
            }
        }
    }

    /**
     * Delete an image from local storage
     * @param localPath Absolute path to the image file
     */
    suspend fun deleteImage(localPath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (file.exists()) {
                    file.delete()
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to delete image")
            }
        }
    }

    /**
     * Get image file from local path
     * @param localPath Absolute path to the image file
     * @return File object
     */
    fun getImageFile(localPath: String): File {
        return File(localPath)
    }

    /**
     * Check if image file exists
     * @param localPath Absolute path to the image file
     */
    fun imageExists(localPath: String): Boolean {
        return File(localPath).exists()
    }

    /**
     * Get total storage used by images (in bytes)
     */
    suspend fun getTotalStorageUsed(): Long {
        return withContext(Dispatchers.IO) {
            try {
                calculateDirectorySize(imagesDir)
            } catch (e: Exception) {
                0L
            }
        }
    }

    /**
     * Delete all images in a directory (for hike deletion)
     * @param targetDirectory Subdirectory (e.g., "hikes/{hikeId}")
     */
    suspend fun deleteDirectory(targetDirectory: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val targetDir = File(imagesDir, targetDirectory)
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to delete directory")
            }
        }
    }

    /**
     * Copy local images to prepare for cloud upload (for migration)
     * @param localPaths List of local absolute paths
     * @return List of URIs ready for upload
     */
    suspend fun prepareForCloudUpload(localPaths: List<String>): List<Uri> {
        return withContext(Dispatchers.IO) {
            localPaths.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) {
                    Uri.fromFile(file)
                } else {
                    null
                }
            }
        }
    }

    /**
     * Calculate directory size recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    /**
     * Clean up old synced images (called after successful migration)
     * Only deletes images older than 30 days that have been synced
     */
    suspend fun cleanupOldSyncedImages(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                var deletedCount = 0

                fun cleanDirectory(dir: File) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isDirectory) {
                            cleanDirectory(file)
                            // Delete empty directories
                            if (file.listFiles()?.isEmpty() == true) {
                                file.delete()
                            }
                        } else {
                            // Delete files older than 30 days
                            if (file.lastModified() < thirtyDaysAgo) {
                                if (file.delete()) {
                                    deletedCount++
                                }
                            }
                        }
                    }
                }

                cleanDirectory(imagesDir)
                Result.Success(deletedCount)
            } catch (e: Exception) {
                Result.Error(e.message ?: "Failed to cleanup old images")
            }
        }
    }
}
