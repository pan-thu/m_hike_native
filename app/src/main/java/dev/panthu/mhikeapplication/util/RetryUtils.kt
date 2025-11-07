package dev.panthu.mhikeapplication.util

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Utility for retry logic with exponential backoff
 *
 * Implements retry mechanism for transient failures in Firebase operations
 * with configurable backoff strategy to avoid overwhelming the server.
 */
object RetryUtils {
    private const val TAG = "RetryUtils"

    /**
     * Retry configuration
     */
    data class RetryConfig(
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 1000L,
        val maxDelayMs: Long = 10000L,
        val factor: Double = 2.0,
        val retryableExceptions: Set<Class<out Exception>> = setOf(
            java.io.IOException::class.java,
            java.net.SocketTimeoutException::class.java,
            java.net.UnknownHostException::class.java,
            javax.net.ssl.SSLException::class.java
        )
    )

    /**
     * Default retry configuration for Firebase operations
     */
    val DEFAULT_CONFIG = RetryConfig()

    /**
     * Aggressive retry configuration for critical operations
     */
    val AGGRESSIVE_CONFIG = RetryConfig(
        maxRetries = 5,
        initialDelayMs = 500L,
        maxDelayMs = 30000L,
        factor = 2.5
    )

    /**
     * Execute a suspend function with retry logic and exponential backoff
     *
     * @param config Retry configuration
     * @param operation Name of operation for logging
     * @param block Suspend function to execute
     * @return Result of the operation
     * @throws Exception if all retries fail
     */
    suspend fun <T> retryWithBackoff(
        config: RetryConfig = DEFAULT_CONFIG,
        operation: String = "operation",
        block: suspend () -> T
    ): T {
        var currentDelay = config.initialDelayMs
        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                val result = block()
                if (attempt > 0) {
                    Log.i(TAG, "$operation succeeded on attempt ${attempt + 1}")
                }
                return result
            } catch (e: Exception) {
                lastException = e

                // Check if exception is retryable
                val isRetryable = config.retryableExceptions.any { it.isInstance(e) } ||
                                 isTransientError(e)

                if (!isRetryable) {
                    Log.w(TAG, "$operation failed with non-retryable error: ${e::class.simpleName}")
                    throw e
                }

                // Don't delay on last attempt
                if (attempt < config.maxRetries - 1) {
                    Log.w(
                        TAG,
                        "$operation failed on attempt ${attempt + 1}/${config.maxRetries}, " +
                        "retrying in ${currentDelay}ms: ${e::class.simpleName} - ${e.message}"
                    )
                    delay(currentDelay)
                    currentDelay = min(
                        (currentDelay * config.factor).toLong(),
                        config.maxDelayMs
                    )
                } else {
                    Log.e(TAG, "$operation failed after ${config.maxRetries} attempts", e)
                }
            }
        }

        // All retries exhausted
        throw lastException ?: Exception("$operation failed after ${config.maxRetries} retries")
    }

    /**
     * Check if an exception represents a transient error worth retrying
     *
     * @param exception The exception to check
     * @return true if the error is likely transient
     */
    private fun isTransientError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""

        // Firebase-specific transient errors
        val transientKeywords = listOf(
            "timeout",
            "unavailable",
            "deadline",
            "network",
            "connection",
            "temporary",
            "throttled",
            "rate limit",
            "503", // Service Unavailable
            "504", // Gateway Timeout
            "429"  // Too Many Requests
        )

        return transientKeywords.any { message.contains(it) }
    }

    /**
     * Wrapper for Result<T> operations with retry logic
     *
     * @param config Retry configuration
     * @param operation Name of operation for logging
     * @param block Suspend function that returns Result<T>
     * @return Result<T>
     */
    suspend fun <T> retryResultOperation(
        config: RetryConfig = DEFAULT_CONFIG,
        operation: String = "operation",
        block: suspend () -> Result<T>
    ): Result<T> {
        return try {
            retryWithBackoff(config, operation) {
                val result = block()
                when (result) {
                    is Result.Success -> result
                    is Result.Error -> throw result.exception
                    is Result.Loading -> result // Don't retry loading state
                }
            }
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "$operation failed after retries")
        }
    }

    /**
     * Calculate jittered delay to prevent thundering herd problem
     *
     * Adds randomness to retry delays when multiple clients retry simultaneously
     *
     * @param baseDelayMs Base delay in milliseconds
     * @param jitterFactor Jitter factor (0.0 to 1.0), default 0.1 (10% jitter)
     * @return Jittered delay
     */
    fun calculateJitteredDelay(baseDelayMs: Long, jitterFactor: Double = 0.1): Long {
        val jitter = (Math.random() * 2 - 1) * baseDelayMs * jitterFactor
        return (baseDelayMs + jitter).toLong().coerceAtLeast(0)
    }
}
