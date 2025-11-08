package dev.panthu.mhikeapplication.domain.model

data class AccessControl(
    val sharedWith: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sharedWith" to sharedWith
    )

    fun hasAccess(userId: String): Boolean {
        return userId in sharedWith
    }

    fun addUser(userId: String): AccessControl {
        return if (userId !in sharedWith) {
            copy(sharedWith = sharedWith + userId)
        } else {
            this
        }
    }

    fun removeUser(userId: String): AccessControl {
        return copy(
            sharedWith = sharedWith.filter { it != userId }
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any>): AccessControl {
            // Support both old format (invitedUsers/sharedUsers) and new format (sharedWith)
            val sharedWith = (data["sharedWith"] as? List<*>)?.mapNotNull { it as? String }
                ?: run {
                    // Migrate old data by combining both lists
                    val invited = (data["invitedUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val shared = (data["sharedUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    (invited + shared).distinct()
                }

            return AccessControl(sharedWith = sharedWith)
        }
    }
}
