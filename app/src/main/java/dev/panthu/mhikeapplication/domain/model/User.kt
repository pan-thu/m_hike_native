package dev.panthu.mhikeapplication.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val handle: String = "", // unique username
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val lastLogin: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "handle" to handle,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isActive" to isActive,
        "metadata" to mapOf(
            "lastLogin" to lastLogin
        )
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): User {
            val metadata = data["metadata"] as? Map<*, *>
            return User(
                uid = data["uid"] as? String ?: "",
                email = data["email"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                handle = data["handle"] as? String ?: "",
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now(),
                isActive = data["isActive"] as? Boolean ?: true,
                lastLogin = metadata?.get("lastLogin") as? Timestamp
            )
        }
    }
}
