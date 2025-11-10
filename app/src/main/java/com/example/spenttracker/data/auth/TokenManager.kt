package com.example.spenttracker.data.auth

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token Manager - Handles token expiry detection and refresh timing
 * Like Laravel's token guard but for client-side token lifecycle management
 */
@Singleton
class TokenManager @Inject constructor(
    private val tokenStorage: AuthTokenStorage
) {
    
    companion object {
        private const val TAG = "TokenManager"
        private const val REFRESH_BUFFER_MINUTES = 5 // Refresh 5 minutes before expiry
        private const val DEFAULT_TOKEN_LIFETIME_HOURS = 24 // Default if no expiry info
    }
    
    private var tokenScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var expiryCheckJob: Job? = null
    
    // Token expiry state
    private val _tokenExpiryState = MutableStateFlow<TokenExpiryState>(TokenExpiryState.Unknown)
    val tokenExpiryState: StateFlow<TokenExpiryState> = _tokenExpiryState.asStateFlow()
    
    // Callbacks
    private var onTokenExpiringSoon: (() -> Unit)? = null
    private var onTokenExpired: (() -> Unit)? = null
    
    /**
     * Store token with expiry information
     */
    fun storeTokenWithExpiry(
        token: String,
        expiresIn: Long? = null,
        expiresAt: String? = null
    ) {
        Log.d(TAG, "Storing token with expiry info: expiresIn=$expiresIn, expiresAt=$expiresAt")
        
        // Calculate expiry timestamp
        val expiryTimestamp = calculateExpiryTimestamp(expiresIn, expiresAt)
        
        // Save token and expiry to storage
        tokenStorage.saveToken(token)
        tokenStorage.saveTokenExpiry(expiryTimestamp)
        
        // Start monitoring expiry
        startExpiryMonitoring()
        
        Log.d(TAG, "Token stored with expiry at: ${Instant.ofEpochMilli(expiryTimestamp)}")
    }
    
    /**
     * Check if current token is expired
     */
    fun isTokenExpired(): Boolean {
        val expiryTime = tokenStorage.getTokenExpiry()
        if (expiryTime == null) {
            Log.w(TAG, "No token expiry information available")
            return false
        }
        
        val isExpired = System.currentTimeMillis() >= expiryTime
        Log.d(TAG, "Token expired check: $isExpired (expires at: ${Instant.ofEpochMilli(expiryTime)})")
        return isExpired
    }
    
    /**
     * Check if token is expiring soon (within buffer time)
     */
    fun isTokenExpiringSoon(): Boolean {
        val expiryTime = tokenStorage.getTokenExpiry()
        if (expiryTime == null) return false
        
        val bufferMs = REFRESH_BUFFER_MINUTES * 60 * 1000L
        val isExpiringSoon = System.currentTimeMillis() >= (expiryTime - bufferMs)
        
        if (isExpiringSoon) {
            Log.w(TAG, "Token expiring soon! Expires at: ${Instant.ofEpochMilli(expiryTime)}")
        }
        
        return isExpiringSoon
    }
    
    /**
     * Get time until token expiry in milliseconds
     */
    fun getTimeUntilExpiry(): Long {
        val expiryTime = tokenStorage.getTokenExpiry()
        if (expiryTime == null) return Long.MAX_VALUE
        
        return maxOf(0L, expiryTime - System.currentTimeMillis())
    }
    
    /**
     * Get time until token needs refresh (expiry - buffer)
     */
    fun getTimeUntilRefreshNeeded(): Long {
        val expiryTime = tokenStorage.getTokenExpiry()
        if (expiryTime == null) return Long.MAX_VALUE
        
        val bufferMs = REFRESH_BUFFER_MINUTES * 60 * 1000L
        val refreshTime = expiryTime - bufferMs
        
        return maxOf(0L, refreshTime - System.currentTimeMillis())
    }
    
    /**
     * Start monitoring token expiry
     */
    private fun startExpiryMonitoring() {
        expiryCheckJob?.cancel()
        
        expiryCheckJob = tokenScope.launch {
            try {
                while (isActive) {
                    when {
                        isTokenExpired() -> {
                            Log.w(TAG, "Token has expired!")
                            _tokenExpiryState.value = TokenExpiryState.Expired
                            
                            withContext(Dispatchers.Main) {
                                onTokenExpired?.invoke()
                            }
                            break
                        }
                        
                        isTokenExpiringSoon() -> {
                            Log.w(TAG, "Token expiring soon - refresh needed")
                            _tokenExpiryState.value = TokenExpiryState.ExpiringSoon
                            
                            withContext(Dispatchers.Main) {
                                onTokenExpiringSoon?.invoke()
                            }
                            
                            // Continue monitoring for actual expiry
                            delay(30000) // Check every 30 seconds when expiring soon
                        }
                        
                        else -> {
                            _tokenExpiryState.value = TokenExpiryState.Valid
                            
                            // Check again closer to expiry time
                            val timeUntilRefresh = getTimeUntilRefreshNeeded()
                            val checkInterval = minOf(timeUntilRefresh / 2, 300000L) // Max 5 minutes
                            
                            delay(maxOf(checkInterval, 60000L)) // Min 1 minute
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Token expiry monitoring cancelled")
            }
        }
    }
    
    /**
     * Calculate expiry timestamp from response data
     */
    private fun calculateExpiryTimestamp(expiresIn: Long?, expiresAt: String?): Long {
        return when {
            // Use explicit expiry time if provided
            !expiresAt.isNullOrEmpty() -> {
                try {
                    val instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(expiresAt))
                    instant.toEpochMilli()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse expiresAt: $expiresAt", e)
                    // Fallback to current time + expiresIn
                    System.currentTimeMillis() + (expiresIn ?: (DEFAULT_TOKEN_LIFETIME_HOURS * 3600L)) * 1000L
                }
            }
            
            // Use relative expiry time if provided
            expiresIn != null -> {
                System.currentTimeMillis() + (expiresIn * 1000L)
            }
            
            // Default fallback - 24 hours from now
            else -> {
                System.currentTimeMillis() + (DEFAULT_TOKEN_LIFETIME_HOURS * 3600L * 1000L)
            }
        }
    }
    
    /**
     * Set callback for when token is expiring soon
     */
    fun setOnTokenExpiringSoon(callback: () -> Unit) {
        onTokenExpiringSoon = callback
    }
    
    /**
     * Set callback for when token has expired
     */
    fun setOnTokenExpired(callback: () -> Unit) {
        onTokenExpired = callback
    }
    
    /**
     * Stop monitoring and cleanup
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping token expiry monitoring")
        expiryCheckJob?.cancel()
        _tokenExpiryState.value = TokenExpiryState.Unknown
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up token manager")
        expiryCheckJob?.cancel()
        tokenScope.cancel()
    }
}

/**
 * Token expiry states
 */
sealed class TokenExpiryState {
    object Unknown : TokenExpiryState()
    object Valid : TokenExpiryState()
    object ExpiringSoon : TokenExpiryState()
    object Expired : TokenExpiryState()
}