package com.example.spenttracker.data.auth

import android.util.Log
import com.example.spenttracker.data.network.ConnectivityStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Manager for handling user session state
 * Sessions persist until user manually logs out
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionConfig: SessionConfig,
    private val networkConnectivityObserver: com.example.spenttracker.data.network.NetworkConnectivityObserver
) {

    companion object {
        private const val TAG = "SessionManager"
    }

    // Session state
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()
    
    /**
     * Start session tracking - called after successful login
     * Sessions now persist until manual logout
     */
    fun startSession() {
        Log.d(TAG, "Starting persistent session (no auto-logout)")
        _isSessionActive.value = true
        _sessionExpired.value = false
    }

    /**
     * End session - called during logout
     */
    fun endSession() {
        Log.d(TAG, "Ending session")
        _isSessionActive.value = false
        _sessionExpired.value = false
    }

    /**
     * Update last activity time - kept for compatibility but no longer affects session timeout
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun updateActivity() {
        // No-op: Sessions persist until manual logout
        Log.v(TAG, "Activity tracked (sessions don't expire)")
    }

    /**
     * Configure session timeout - kept for compatibility but no longer used
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun setSessionTimeout(timeoutMs: Long) {
        Log.w(TAG, "setSessionTimeout called but sessions no longer expire automatically")
    }

    /**
     * Set callback for when session expires - kept for compatibility but no longer used
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun setOnSessionExpired(callback: () -> Unit) {
        Log.w(TAG, "setOnSessionExpired called but sessions no longer expire automatically")
    }

    /**
     * Get remaining session time - always returns Long.MAX_VALUE (infinite)
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun getRemainingSessionTime(): Long {
        return if (_isSessionActive.value) Long.MAX_VALUE else 0L
    }

    /**
     * Check if session is about to expire - always returns false
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun isSessionAboutToExpire(): Boolean {
        return false
    }

    /**
     * Extend current session - no longer needed
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun extendSession() {
        Log.v(TAG, "extendSession called but sessions don't expire")
    }

    /**
     * Get session timeout in minutes - returns 0 (disabled)
     * @deprecated No longer needed as sessions don't expire
     */
    @Deprecated("Sessions no longer expire automatically")
    fun getSessionTimeoutMinutes(): Int {
        return 0 // Timeout disabled
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up session manager")
    }
}