package com.example.spenttracker.data.remote.auth

import com.example.spenttracker.data.auth.AuthTokenStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth Token Provider Implementation
 * Gets the current auth token with persistent storage support
 */
@Singleton
class AuthTokenProviderImpl @Inject constructor(
    private val tokenStorage: AuthTokenStorage
) : AuthTokenProvider {
    
    private var currentToken: String? = null
    
    init {
        // Load token from persistent storage on startup
        currentToken = tokenStorage.getToken()
        android.util.Log.d("AuthTokenProvider", "Initialized with stored token: ${if (currentToken != null) "found" else "not found"}")
    }
    
    override fun getAuthToken(): String? {
        // Return in-memory token if available, otherwise check storage
        return currentToken ?: tokenStorage.getToken()?.also {
            currentToken = it // Cache it in memory
        }
    }
    
    /**
     * Called by AuthViewModel when token changes
     * Saves token both in memory and persistent storage
     */
    fun updateToken(token: String?) {
        currentToken = token
        
        if (token != null) {
            tokenStorage.saveToken(token)
            android.util.Log.d("AuthTokenProvider", "Token updated and saved to persistent storage")
        } else {
            tokenStorage.clearAll()
            android.util.Log.d("AuthTokenProvider", "Token cleared from memory and persistent storage")
        }
    }
}