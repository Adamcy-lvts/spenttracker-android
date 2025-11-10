package com.example.spenttracker.data.auth

import android.util.Log
import com.example.spenttracker.data.remote.api.AuthApiService
import com.example.spenttracker.data.remote.auth.AuthTokenProviderImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Token Refresh Manager - Automatically refreshes tokens before expiry
 * 
 * How it works:
 * 1. TokenManager detects token expiring soon (5 mins before)
 * 2. This manager calls Laravel /refresh endpoint
 * 3. Gets new token with fresh expiry time
 * 4. Updates all storage seamlessly
 * 5. User never knows token was refreshed!
 */
@Singleton
class TokenRefreshManager @Inject constructor(
    @Named("refresh") private val refreshApiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val authTokenProvider: AuthTokenProviderImpl,
    private val tokenStorage: AuthTokenStorage,
    private val userContextProvider: UserContextProvider
) {
    
    companion object {
        private const val TAG = "TokenRefreshManager"
        private const val MAX_REFRESH_RETRIES = 3
        private const val RETRY_DELAY_MS = 5000L // 5 seconds
    }
    
    private var refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null
    private var refreshInProgress = false
    
    // Refresh state
    private val _refreshState = MutableStateFlow<TokenRefreshState>(TokenRefreshState.Idle)
    val refreshState: StateFlow<TokenRefreshState> = _refreshState.asStateFlow()
    
    // Callbacks
    private var onRefreshSuccess: ((String) -> Unit)? = null
    private var onRefreshFailed: ((String) -> Unit)? = null
    
    init {
        // Listen for token expiry events
        setupTokenExpiryListener()
    }
    
    /**
     * Setup listener for token expiry events from TokenManager
     */
    private fun setupTokenExpiryListener() {
        tokenManager.setOnTokenExpiringSoon {
            Log.w(TAG, "Token expiring soon - attempting automatic refresh")
            attemptTokenRefresh()
        }
        
        tokenManager.setOnTokenExpired {
            Log.e(TAG, "Token has expired - refresh may no longer be possible")
            _refreshState.value = TokenRefreshState.Failed("Token expired")
            onRefreshFailed?.invoke("Token expired - please login again")
        }
    }
    
    /**
     * Attempt to refresh the current token
     */
    fun attemptTokenRefresh() {
        if (refreshInProgress) {
            Log.d(TAG, "Token refresh already in progress")
            return
        }
        
        refreshJob?.cancel()
        refreshJob = refreshScope.launch {
            refreshInProgress = true
            _refreshState.value = TokenRefreshState.Refreshing
            
            try {
                val success = performTokenRefreshWithRetries()
                
                if (success) {
                    _refreshState.value = TokenRefreshState.Success
                    Log.i(TAG, "Token refresh completed successfully")
                } else {
                    _refreshState.value = TokenRefreshState.Failed("Max retries exceeded")
                    Log.e(TAG, "Token refresh failed after max retries")
                    
                    withContext(Dispatchers.Main) {
                        onRefreshFailed?.invoke("Failed to refresh token - please login again")
                    }
                }
            } catch (e: Exception) {
                _refreshState.value = TokenRefreshState.Failed(e.message ?: "Unknown error")
                Log.e(TAG, "Token refresh failed with exception", e)
                
                withContext(Dispatchers.Main) {
                    onRefreshFailed?.invoke("Network error during token refresh")
                }
            } finally {
                refreshInProgress = false
            }
        }
    }
    
    /**
     * Perform token refresh with retry logic
     */
    private suspend fun performTokenRefreshWithRetries(): Boolean {
        repeat(MAX_REFRESH_RETRIES) { attempt ->
            try {
                Log.d(TAG, "Token refresh attempt ${attempt + 1}/$MAX_REFRESH_RETRIES")
                
                val response = refreshApiService.refreshToken()
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Successfully got new token
                        val newToken = authResponse.accessToken ?: authResponse.token
                        
                        Log.i(TAG, "New token received from refresh endpoint")
                        
                        // Update token storage
                        authTokenProvider.updateToken(newToken)
                        
                        // Update expiry information if provided
                        tokenManager.storeTokenWithExpiry(
                            token = newToken,
                            expiresIn = authResponse.expiresIn?.toLong(),
                            expiresAt = authResponse.expiresAt
                        )
                        
                        Log.i(TAG, "Token refresh successful - new token stored with expiry")
                        
                        withContext(Dispatchers.Main) {
                            onRefreshSuccess?.invoke(newToken)
                        }
                        
                        return true
                    } else {
                        Log.w(TAG, "Refresh response successful but no body")
                    }
                } else {
                    when (response.code()) {
                        401 -> {
                            Log.e(TAG, "Token refresh failed - token invalid (401)")
                            return false // Don't retry on 401
                        }
                        403 -> {
                            Log.e(TAG, "Token refresh failed - forbidden (403)")
                            return false // Don't retry on 403
                        }
                        else -> {
                            Log.w(TAG, "Token refresh failed with code: ${response.code()}")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Token refresh attempt ${attempt + 1} failed: ${e.message}")
                
                // Don't retry on certain exceptions
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }
            }
            
            // Wait before retry (except on last attempt)
            if (attempt < MAX_REFRESH_RETRIES - 1) {
                delay(RETRY_DELAY_MS)
            }
        }
        
        return false
    }
    
    /**
     * Check if token refresh is currently in progress
     */
    fun isRefreshInProgress(): Boolean = refreshInProgress
    
    /**
     * Set callback for successful token refresh
     */
    fun setOnRefreshSuccess(callback: (String) -> Unit) {
        onRefreshSuccess = callback
    }
    
    /**
     * Set callback for failed token refresh
     */
    fun setOnRefreshFailed(callback: (String) -> Unit) {
        onRefreshFailed = callback
    }
    
    /**
     * Stop any ongoing refresh operations
     */
    fun stopRefresh() {
        Log.d(TAG, "Stopping token refresh operations")
        refreshJob?.cancel()
        refreshInProgress = false
        _refreshState.value = TokenRefreshState.Idle
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up token refresh manager")
        refreshJob?.cancel()
        refreshScope.cancel()
    }
}

/**
 * Token refresh states
 */
sealed class TokenRefreshState {
    object Idle : TokenRefreshState()
    object Refreshing : TokenRefreshState()
    object Success : TokenRefreshState()
    data class Failed(val error: String) : TokenRefreshState()
}