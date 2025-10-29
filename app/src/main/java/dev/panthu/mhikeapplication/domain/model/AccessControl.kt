package dev.panthu.mhikeapplication.domain.model

data class AccessControl(
    val invitedUsers: List<String> = emptyList(),
    val sharedUsers: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "invitedUsers" to invitedUsers,
        "sharedUsers" to sharedUsers
    )

    fun hasAccess(userId: String): Boolean {
        return userId in invitedUsers || userId in sharedUsers
    }

    fun addInvitedUser(userId: String): AccessControl {
        return if (userId !in invitedUsers) {
            copy(invitedUsers = invitedUsers + userId)
        } else {
            this
        }
    }

    fun addSharedUser(userId: String): AccessControl {
        return if (userId !in sharedUsers) {
            copy(sharedUsers = sharedUsers + userId)
        } else {
            this
        }
    }

    fun removeUser(userId: String): AccessControl {
        return copy(
            invitedUsers = invitedUsers.filter { it != userId },
            sharedUsers = sharedUsers.filter { it != userId }
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any>): AccessControl {
            return AccessControl(
                invitedUsers = (data["invitedUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                sharedUsers = (data["sharedUsers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
    }
}
