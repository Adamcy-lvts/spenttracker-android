package com.example.spenttracker.data.auth

import android.util.Log
import com.example.spenttracker.data.preferences.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User Context Provider Implementation
 * Manages current user ID for data filtering with offline session restore capability
 */
@Singleton
class UserContextProviderImpl @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : UserContextProvider {
    
    companion object {
        private const val TAG = "UserContextProvider"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserIdFlow: StateFlow<Long?> = _currentUserId.asStateFlow()
    
    private val _isSessionRestored = MutableStateFlow(false)
    val isSessionRestored: StateFlow<Boolean> = _isSessionRestored.asStateFlow()
    
    override fun getCurrentUserId(): Long? {
        val userId = _currentUserId.value
        Log.d(TAG, "getCurrentUserId() returning: $userId")
        return userId
    }
    
    /**
     * Called by AuthViewModel when user logs in/out
     */
    fun updateUserId(userId: Long?) {
        Log.d(TAG, "updateUserId() called: ${_currentUserId.value} -> $userId")
        _currentUserId.value = userId
    }
    
    /**
     * Restore user session from stored preferences
     * Call this on app startup to enable offline access
     */
    fun restoreStoredSession() {
        Log.d(TAG, "Attempting to restore stored session...")
        
        scope.launch {
            try {
                val userData = userPreferencesManager.getUserData().first()
                
                if (userData.isLoggedIn && userData.userId.isNotEmpty()) {
                    val userId = userData.userId.toLongOrNull()
                    
                    if (userId != null) {
                        Log.i(TAG, "Restoring session for user: ${userData.userName} (ID: $userId)")
                        _currentUserId.value = userId
                        _isSessionRestored.value = true
                        Log.i(TAG, "Session restored successfully - offline access enabled")
                    } else {
                        Log.w(TAG, "Invalid stored user ID: ${userData.userId}")
                        _isSessionRestored.value = true // Mark as attempted
                    }
                } else {
                    Log.d(TAG, "No valid stored session found")
                    _isSessionRestored.value = true // Mark as attempted
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore session: ${e.message}", e)
                _isSessionRestored.value = true // Mark as attempted even on error
            }
        }
    }
    
    /**
     * Check if user has a valid stored session (for offline access)
     */
    suspend fun hasValidStoredSession(): Boolean {
        return try {
            val userData = userPreferencesManager.getUserData().first()
            val hasValidSession = userData.isLoggedIn && 
                userData.userId.isNotEmpty() && 
                userData.userId.toLongOrNull() != null
                
            Log.d(TAG, "hasValidStoredSession: $hasValidSession (userId: ${userData.userId}, isLoggedIn: ${userData.isLoggedIn})")
            hasValidSession
        } catch (e: Exception) {
            Log.e(TAG, "Error checking stored session: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get stored user data without updating current session
     */
    suspend fun getStoredUserData(): com.example.spenttracker.data.preferences.UserData? {
        return try {
            val userData = userPreferencesManager.getUserData().first()
            if (userData.isLoggedIn) userData else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stored user data: ${e.message}", e)
            null
        }
    }
    
    /**
     * Clear current session (but keep stored data for potential restore)
     */
    fun clearCurrentSession() {
        Log.d(TAG, "Clearing current session")
        _currentUserId.value = null
        _isSessionRestored.value = false
    }
}