package com.example.spenttracker.data.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session Configuration Manager
 * Note: Session timeout functionality has been disabled - sessions now persist until manual logout
 * This class is kept for backward compatibility
 */
@Singleton
class SessionConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "session_config"
        private const val KEY_SESSION_TIMEOUT_MINUTES = "session_timeout_minutes"
        private const val KEY_WARN_BEFORE_EXPIRY = "warn_before_expiry"
        private const val KEY_AUTO_EXTEND_ON_ACTIVITY = "auto_extend_on_activity"

        // Default values - kept for backward compatibility but not used
        const val DEFAULT_SESSION_TIMEOUT_MINUTES = 0 // Disabled
        const val DEFAULT_WARN_BEFORE_EXPIRY = false
        const val DEFAULT_AUTO_EXTEND_ON_ACTIVITY = false
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get session timeout in minutes - always returns 0 (disabled)
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun getSessionTimeoutMinutes(): Int {
        return 0 // Always disabled
    }

    /**
     * Set session timeout in minutes - no-op
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun setSessionTimeoutMinutes(minutes: Int) {
        android.util.Log.w("SessionConfig", "Session timeout is disabled - ignoring timeout configuration")
    }

    /**
     * Get session timeout in milliseconds - always returns 0 (disabled)
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun getSessionTimeoutMs(): Long {
        return 0L // Always disabled
    }

    /**
     * Check if should warn before session expiry - always returns false
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun shouldWarnBeforeExpiry(): Boolean {
        return false
    }

    /**
     * Set whether to warn before session expiry - no-op
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun setWarnBeforeExpiry(warn: Boolean) {
        // No-op
    }

    /**
     * Check if should auto-extend session on user activity - always returns false
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun shouldAutoExtendOnActivity(): Boolean {
        return false
    }

    /**
     * Set whether to auto-extend session on user activity - no-op
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun setAutoExtendOnActivity(autoExtend: Boolean) {
        // No-op
    }
    
    /**
     * Get all session configuration as data class - returns disabled configuration
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun getSessionConfiguration(): SessionConfiguration {
        return SessionConfiguration(
            timeoutMinutes = 0,
            warnBeforeExpiry = false,
            autoExtendOnActivity = false
        )
    }

    /**
     * Update session configuration - no-op
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun updateSessionConfiguration(config: SessionConfiguration) {
        android.util.Log.w("SessionConfig", "Session timeout is disabled - ignoring configuration update")
    }

    /**
     * Reset to default configuration - no-op
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun resetToDefaults() {
        android.util.Log.w("SessionConfig", "Session timeout is disabled - no configuration to reset")
    }

    /**
     * Get available timeout options - returns empty list
     * @deprecated Session timeout is disabled
     */
    @Deprecated("Session timeout is disabled")
    fun getAvailableTimeoutOptions(): List<Int> {
        return emptyList() // No timeout options available
    }
}

/**
 * Data class for session configuration
 */
data class SessionConfiguration(
    val timeoutMinutes: Int,
    val warnBeforeExpiry: Boolean,
    val autoExtendOnActivity: Boolean
)