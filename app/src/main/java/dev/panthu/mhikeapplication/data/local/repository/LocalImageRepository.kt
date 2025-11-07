package dev.panthu.mhikeapplication.data.local.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
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

    companion object {
        /**
         * Configurable cleanup threshold in days
         * Default: 30 days
         * Can be overridden via constructor or configuration
         */
        const val DEFAULT_CLEANUP_THRESHOLD_DAYS = 30

        /**
         * Convert days to milliseconds for cleanup calculations
         */
        fun daysToMillis(days: Int): Long = days * 24 * 60 * 60 * 1000L
    }

    init {
        // Ensure images directory exists
        imagesDir.mkdirs()
    }

    /**
     * Verify that a URI is valid and accessible
     * @param uri The URI to verify
     * @return Result indicating validation status
     */
    private fun verifyUri(uri: Uri): Result<Unit> {
        // Validate scheme
        val scheme = uri.scheme
        if (scheme.isNullOrEmpty()) {
            return Result.Error("Invalid URI: missing scheme")
        }

        // Only allow content:// and file:// schemes for security
        if (scheme !in listOf("content", "file")) {
            return Result.Error("Invalid URI scheme: $scheme. Only content:// and file:// are allowed")
        }

        // Validate URI is not empty
        if (uri.toString().isEmpty()) {
            return Result.Error("URI is empty")
        }

        return Result.Success(Unit)
    }

    /**
     * Save an image from URI to local storage with verification
     * @param imageUri Source URI (from gallery/camera)
     * @param targetDirectory Subdirectory (e.g., "hikes/{hikeId}")
     * @return Absolute path to saved image file
     */
    suspend fun saveImage(imageUri: Uri, targetDirectory: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Verify URI before processing
                when (val verificationResult = verifyUri(imageUri)) {
                    is Result.Error -> return@withContext verificationResult
                    else -> {} // Continue processing
                }

                val targetDir = File(imagesDir, targetDirectory)
                targetDir.mkdirs()

                val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val targetFile = File(targetDir, fileName)

                var bytesCopied = 0L
                val inputStream = context.contentResolver.openInputStream(imageUri)

                if (inputStream == null) {
                    return@withContext Result.Error("Unable to open image URI: access denied or file not found")
                }

                inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        bytesCopied = input.copyTo(output)
                    }
                }

                // Verify file was created successfully
                if (!targetFile.exists()) {
                    return@withContext Result.Error("File creation failed")
                }

                // Verify data was written
                if (bytesCopied == 0L) {
                    targetFile.delete()
                    return@withContext Result.Error("No data copied from URI")
                }

                // Final verification - check file size matches bytes copied
                val fileSize = targetFile.length()
                if (fileSize != bytesCopied) {
                    targetFile.delete()
                    return@withContext Result.Error("File size mismatch: expected $bytesCopied, got $fileSize")
                }

                Result.Success(targetFile.absolutePath)
            } catch (e: Exception) {
                val errorMsg = "Failed to save image from URI ${imageUri.lastPathSegment ?: "unknown"}: ${e.message}"
                android.util.Log.e("LocalImageRepository", errorMsg, e)
                Result.Error(errorMsg)
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
                if (!file.exists()) {
                    android.util.Log.v("LocalImageRepository", "File already deleted: ${file.name}")
                    return@withContext Result.Success(Unit)
                }

                val deleted = file.delete()
                if (deleted) {
                    android.util.Log.v("LocalImageRepository", "Successfully deleted: ${file.name}")
                    Result.Success(Unit)
                } else {
                    val errorMsg = "Failed to delete '${file.name}': File may be in use or insufficient permissions (path: $localPath)"
                    android.util.Log.w("LocalImageRepository", errorMsg)
                    Result.Error(errorMsg)
                }
            } catch (e: SecurityException) {
                val errorMsg = "Permission denied deleting '${File(localPath).name}': ${e.message}"
                android.util.Log.e("LocalImageRepository", errorMsg, e)
                Result.Error(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "Unexpected error deleting '${File(localPath).name}': ${e.message}"
                android.util.Log.e("LocalImageRepository", errorMsg, e)
                Result.Error(errorMsg)
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
                if (!targetDir.exists()) {
                    android.util.Log.v("LocalImageRepository", "Directory already deleted: $targetDirectory")
                    return@withContext Result.Success(Unit)
                }

                val fileCount = targetDir.walk().count()
                val success = targetDir.deleteRecursively()

                if (success) {
                    android.util.Log.i("LocalImageRepository", "Successfully deleted directory '$targetDirectory' ($fileCount items)")
                    Result.Success(Unit)
                } else {
                    val errorMsg = "Failed to delete directory '$targetDirectory': Some files may be in use"
                    android.util.Log.w("LocalImageRepository", errorMsg)
                    Result.Error(errorMsg)
                }
            } catch (e: SecurityException) {
                val errorMsg = "Permission denied deleting directory '$targetDirectory': ${e.message}"
                android.util.Log.e("LocalImageRepository", errorMsg, e)
                Result.Error(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "Unexpected error deleting directory '$targetDirectory': ${e.message}"
                android.util.Log.e("LocalImageRepository", errorMsg, e)
                Result.Error(errorMsg)
            }
        }
    }

    /**
     * Copy local images to prepare for cloud upload (for migration)
     * @param localPaths List of local absolute paths
     * @return List of URIs ready for upload using FileProvider
     * Memory-optimized: Pre-allocates list capacity and uses sequence for lazy evaluation
     * CRITICAL FIX: Uses FileProvider instead of Uri.fromFile() for Android 7.1+ compatibility
     */
    suspend fun prepareForCloudUpload(localPaths: List<String>): List<Uri> {
        return withContext(Dispatchers.IO) {
            // Pre-allocate ArrayList with expected capacity for better memory efficiency
            val uris = ArrayList<Uri>(localPaths.size)

            localPaths.forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    try {
                        // CRITICAL FIX: Use FileProvider instead of Uri.fromFile()
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        uris.add(uri)
                    } catch (e: IllegalArgumentException) {
                        android.util.Log.e(
                            "LocalImageRepository",
                            "Failed to create FileProvider URI for: $path",
                            e
                        )
                        // Skip this file if FileProvider fails
                    }
                }
            }

            // Return immutable list to prevent further modifications
            uris.toList()
        }
    }

    /**
     * Calculate directory size recursively with null safety
     * @param directory The directory to calculate size for
     * @return Total size in bytes, or 0 if directory is null/invalid
     */
    private fun calculateDirectorySize(directory: File?): Long {
        // Null safety check
        if (directory == null || !directory.exists()) {
            android.util.Log.w("LocalImageRepository", "Cannot calculate size: directory is null or doesn't exist")
            return 0L
        }

        var size = 0L
        if (directory.isDirectory) {
            // Null check for listFiles()
            val files = directory.listFiles()
            if (files == null) {
                android.util.Log.w("LocalImageRepository", "Cannot list files in directory: ${directory.path}")
                return 0L
            }

            files.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    // Null-safe file.length() - returns 0 if file doesn't exist
                    try {
                        file.length()
                    } catch (e: SecurityException) {
                        android.util.Log.w("LocalImageRepository", "Cannot access file: ${file.path}")
                        0L
                    }
                }
            }
        } else {
            // If it's a file, return its size
            try {
                size = directory.length()
            } catch (e: SecurityException) {
                android.util.Log.w("LocalImageRepository", "Cannot access file: ${directory.path}")
            }
        }
        return size
    }

    /**
     * Clean up old synced images (called after successful migration)
     * Only deletes images older than the threshold that have been synced
     * Uses iterative approach to prevent stack overflow with deep directory structures
     *
     * @param thresholdDays Number of days to keep images (default: 30 days)
     * @return Result with count of deleted files
     */
    suspend fun cleanupOldSyncedImages(
        thresholdDays: Int = DEFAULT_CLEANUP_THRESHOLD_DAYS
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val cutoffTimestamp = System.currentTimeMillis() - daysToMillis(thresholdDays)
                var deletedCount = 0

                android.util.Log.d("LocalImageRepository", "Starting cleanup with threshold: $thresholdDays days")

                // Use iterative approach with queue to prevent stack overflow
                // Pre-allocate queue with reasonable initial capacity for better memory efficiency
                val queue = ArrayDeque<File>(32)
                queue.add(imagesDir)

                while (queue.isNotEmpty()) {
                    val currentDir = queue.removeFirst()
                    val files = currentDir.listFiles()

                    // Null check for listFiles() which can return null
                    if (files == null) {
                        android.util.Log.w("LocalImageRepository", "Unable to list files in: ${currentDir.path}")
                        continue
                    }

                    // Process files first, collect subdirectories
                    for (file in files) {
                        when {
                            file.isDirectory -> {
                                // Add subdirectory to queue for processing
                                queue.add(file)
                            }
                            file.isFile && file.lastModified() < cutoffTimestamp -> {
                                if (file.delete()) {
                                    deletedCount++
                                    android.util.Log.v("LocalImageRepository", "Deleted old file: ${file.name}")
                                } else {
                                    android.util.Log.w("LocalImageRepository", "Failed to delete old file: ${file.path}")
                                }
                            }
                        }
                    }
                }

                // Clean up empty directories after all files processed
                cleanEmptyDirectories(imagesDir)

                android.util.Log.i("LocalImageRepository", "Cleanup completed: $deletedCount files deleted")
                Result.Success(deletedCount)
            } catch (e: Exception) {
                android.util.Log.e("LocalImageRepository", "Cleanup failed: ${e.message}", e)
                Result.Error(e.message ?: "Failed to cleanup old images")
            }
        }
    }

    /**
     * Helper to clean empty directories
     * Checks if directory is empty using safe null handling
     */
    private fun cleanEmptyDirectories(dir: File) {
        val files = dir.listFiles()

        // Safe null check - null means cannot access directory
        if (files == null) {
            return
        }

        // Recursively clean subdirectories first
        files.forEach { file ->
            if (file.isDirectory) {
                cleanEmptyDirectories(file)
            }
        }

        // After cleaning subdirectories, check if this directory is now empty
        val updatedFiles = dir.listFiles()
        if (updatedFiles != null && updatedFiles.isEmpty() && dir != imagesDir) {
            dir.delete()
        }
    }
}
