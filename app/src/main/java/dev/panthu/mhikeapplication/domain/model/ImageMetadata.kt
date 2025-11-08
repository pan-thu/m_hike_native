package dev.panthu.mhikeapplication.domain.model

import com.google.firebase.Timestamp

data class ImageMetadata(
    val id: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val storagePath: String = "",
    val contentType: String = "image/jpeg",
    val size: Long = 0,
    val uploadedAt: Timestamp = Timestamp.now(),
    val uploadedBy: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "url" to url,
        "thumbnailUrl" to thumbnailUrl,
        "storagePath" to storagePath,
        "contentType" to contentType,
        "size" to size,
        "uploadedAt" to uploadedAt,
        "uploadedBy" to uploadedBy
    )

    companion object {
        fun fromMap(data: Map<String, Any>): ImageMetadata {
            return ImageMetadata(
                id = data["id"] as? String ?: "",
                url = data["url"] as? String ?: "",
                thumbnailUrl = data["thumbnailUrl"] as? String ?: "",
                storagePath = data["storagePath"] as? String ?: "",
                contentType = data["contentType"] as? String ?: "image/jpeg",
                size = (data["size"] as? Number)?.toLong() ?: 0,
                uploadedAt = data["uploadedAt"] as? Timestamp ?: Timestamp.now(),
                uploadedBy = data["uploadedBy"] as? String ?: ""
            )
        }
    }
}

data class UploadProgress(
    val imageId: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val downloadUrl: String = "",
    val storagePath: String = "",
    val error: Exception? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) bytesTransferred.toFloat() / totalBytes.toFloat() else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
