package dev.panthu.mhikeapplication.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages guest user ID and onboarding state persistence
 */
@Singleton
class GuestIdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences? by lazy {
        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize SharedPreferences", e)
            null
        }
    }

    /**
     * Generate or retrieve existing guest ID with null safety
     */
    fun getOrCreateGuestId(): String {
        return try {
            val existingId = prefs?.getString(KEY_GUEST_ID, null)
            if (existingId != null) {
                existingId
            } else {
                val newId = generateGuestId()
                prefs?.edit()?.putString(KEY_GUEST_ID, newId)?.apply()
                newId
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get or create guest ID", e)
            // Fallback: Generate ID that won't be persisted
            generateGuestId()
        }
    }

    /**
     * Get existing guest ID without creating new one (null-safe)
     */
    fun getGuestId(): String? {
        return try {
            prefs?.getString(KEY_GUEST_ID, null)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get guest ID", e)
            null
        }
    }

    /**
     * Clear guest ID (called after migration to authenticated account)
     */
    fun clearGuestId() {
        try {
            prefs?.edit()?.remove(KEY_GUEST_ID)?.apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to clear guest ID", e)
        }
    }

    /**
     * Check if user has completed onboarding
     */
    fun hasCompletedOnboarding(): Boolean {
        return try {
            prefs?.getBoolean(KEY_ONBOARDING_COMPLETE, false) ?: false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check onboarding status", e)
            false
        }
    }

    /**
     * Mark onboarding as complete
     */
    fun setOnboardingComplete() {
        try {
            prefs?.edit()?.putBoolean(KEY_ONBOARDING_COMPLETE, true)?.apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to set onboarding complete", e)
        }
    }

    /**
     * Check if user was previously a guest (for migration detection)
     */
    fun wasGuest(): Boolean {
        return try {
            prefs?.getBoolean(KEY_WAS_GUEST, false) ?: false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to check was guest status", e)
            false
        }
    }

    /**
     * Mark user as former guest (for migration tracking)
     */
    fun markAsFormerGuest() {
        try {
            prefs?.edit()?.putBoolean(KEY_WAS_GUEST, true)?.apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to mark as former guest", e)
        }
    }

    /**
     * Get the last authentication mode used
     */
    fun getLastAuthMode(): AuthMode {
        return try {
            val mode = prefs?.getString(KEY_LAST_AUTH_MODE, null)
            when (mode) {
                "guest" -> AuthMode.GUEST
                "authenticated" -> AuthMode.AUTHENTICATED
                else -> AuthMode.NONE
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get last auth mode", e)
            AuthMode.NONE
        }
    }

    /**
     * Save the current authentication mode
     */
    fun setLastAuthMode(mode: AuthMode) {
        try {
            val modeString = when (mode) {
                AuthMode.GUEST -> "guest"
                AuthMode.AUTHENTICATED -> "authenticated"
                AuthMode.NONE -> null
            }
            if (modeString != null) {
                prefs?.edit()?.putString(KEY_LAST_AUTH_MODE, modeString)?.apply()
            } else {
                prefs?.edit()?.remove(KEY_LAST_AUTH_MODE)?.apply()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to set last auth mode", e)
        }
    }

    /**
     * Reset all guest-related data (for testing or fresh start)
     */
    fun resetAll() {
        try {
            prefs?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to reset all data", e)
        }
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
        private const val TAG = "GuestIdManager"
        private const val PREFS_NAME = "mhike_auth_prefs"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_WAS_GUEST = "was_guest"
        private const val KEY_LAST_AUTH_MODE = "last_auth_mode"
    }
}
