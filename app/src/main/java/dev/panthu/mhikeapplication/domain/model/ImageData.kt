package dev.panthu.mhikeapplication.domain.model

/**
 * Represents an image with its storage location metadata
 */
data class ImageData(
    val id: String,
    val url: String, // Either local file path or Firebase URL
    val storageType: StorageType,
    val uploadedAt: Long? = null,
    val size: Long? = null
)

/**
 * Indicates where an image is stored
 */
enum class StorageType {
    /**
     * Image stored locally on device (guest mode)
     */
    LOCAL,

    /**
     * Image stored in Firebase Storage (authenticated mode)
     */
    FIREBASE
}

/**
 * Extension function to determine storage type from URL
 */
fun String.getStorageType(): StorageType {
    return when {
        startsWith("http") || startsWith("https") -> StorageType.FIREBASE
        startsWith("/") || startsWith("file://") -> StorageType.LOCAL
        else -> StorageType.LOCAL // Default to local for safety
    }
}

/**
 * Extension function to create ImageData from URL string
 */
fun String.toImageData(id: String = java.util.UUID.randomUUID().toString()): ImageData {
    return ImageData(
        id = id,
        url = this,
        storageType = this.getStorageType()
    )
}

/**
 * Extension function to convert list of URLs to ImageData list
 */
fun List<String>.toImageDataList(): List<ImageData> {
    return this.mapIndexed { index, url ->
        url.toImageData(id = "img_$index")
    }
}
