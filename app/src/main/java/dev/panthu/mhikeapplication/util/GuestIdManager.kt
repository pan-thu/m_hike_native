package dev.panthu.mhikeapplication.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages guest user ID and onboarding state persistence
 */
@Singleton
class GuestIdManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Generate or retrieve existing guest ID
     */
    fun getOrCreateGuestId(): String {
        val existingId = prefs.getString(KEY_GUEST_ID, null)
        return if (existingId != null) {
            existingId
        } else {
            val newId = generateGuestId()
            prefs.edit().putString(KEY_GUEST_ID, newId).apply()
            newId
        }
    }

    /**
     * Get existing guest ID without creating new one
     */
    fun getGuestId(): String? {
        return prefs.getString(KEY_GUEST_ID, null)
    }

    /**
     * Clear guest ID (called after migration to authenticated account)
     */
    fun clearGuestId() {
        prefs.edit().remove(KEY_GUEST_ID).apply()
    }

    /**
     * Check if user has completed onboarding
     */
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    /**
     * Mark onboarding as complete
     */
    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    /**
     * Check if user was previously a guest (for migration detection)
     */
    fun wasGuest(): Boolean {
        return prefs.getBoolean(KEY_WAS_GUEST, false)
    }

    /**
     * Mark user as former guest (for migration tracking)
     */
    fun markAsFormerGuest() {
        prefs.edit().putBoolean(KEY_WAS_GUEST, true).apply()
    }

    /**
     * Get the last authentication mode used
     */
    fun getLastAuthMode(): AuthMode {
        val mode = prefs.getString(KEY_LAST_AUTH_MODE, null)
        return when (mode) {
            "guest" -> AuthMode.GUEST
            "authenticated" -> AuthMode.AUTHENTICATED
            else -> AuthMode.NONE
        }
    }

    /**
     * Save the current authentication mode
     */
    fun setLastAuthMode(mode: AuthMode) {
        val modeString = when (mode) {
            AuthMode.GUEST -> "guest"
            AuthMode.AUTHENTICATED -> "authenticated"
            AuthMode.NONE -> null
        }
        if (modeString != null) {
            prefs.edit().putString(KEY_LAST_AUTH_MODE, modeString).apply()
        } else {
            prefs.edit().remove(KEY_LAST_AUTH_MODE).apply()
        }
    }

    /**
     * Reset all guest-related data (for testing or fresh start)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }

    private fun generateGuestId(): String {
        return "guest_${UUID.randomUUID()}"
    }

    enum class AuthMode {
        NONE,
        GUEST,
        AUTHENTICATED
    }

    companion object {
        private const val PREFS_NAME = "mhike_auth_prefs"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_WAS_GUEST = "was_guest"
        private const val KEY_LAST_AUTH_MODE = "last_auth_mode"
    }
}
