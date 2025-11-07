package dev.panthu.mhikeapplication.data.local

import android.util.Log
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

/**
 * Type converters for Room database
 */
class Converters {

    companion object {
        private const val TAG = "Converters"
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return try {
            Date(value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert timestamp $value to Date", e)
            Date(0) // Return epoch as fallback
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return try {
            date.time
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert Date to timestamp: $date", e)
            0L // Return epoch as fallback
        }
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }

        return try {
            val list = json.decodeFromString<List<String>>(value)
            // Filter out null or empty strings from deserialized list
            val filtered = list.filterNotNull().filter { it.isNotEmpty() }
            if (filtered.size != list.size) {
                Log.w(TAG, "Filtered ${list.size - filtered.size} null/empty items from deserialized list")
            }
            filtered
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e(TAG, "Serialization error deserializing list from JSON: ${value.take(100)}...", e)
            emptyList()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid argument deserializing list from JSON: ${value.take(100)}...", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error deserializing list from JSON: ${value.take(100)}...", e)
            emptyList()
        }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        if (list.isNullOrEmpty()) {
            return json.encodeToString(emptyList<String>())
        }

        // Filter out null or empty strings before serialization
        val filtered = list.filterNotNull().filter { it.isNotEmpty() }
        if (filtered.size != list.size) {
            Log.w(TAG, "Filtered ${list.size - filtered.size} null/empty items before serialization")
        }

        return try {
            json.encodeToString(filtered)
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e(TAG, "Serialization error converting list to JSON: ${filtered.take(5)}", e)
            "[]" // Return empty array as fallback
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error converting list to JSON: ${filtered.take(5)}", e)
            "[]" // Return empty array as fallback
        }
    }
}
