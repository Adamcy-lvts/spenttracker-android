package com.example.spenttracker.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure Token Storage using EncryptedSharedPreferences
 * Handles encrypted storage and retrieval of authentication tokens and user data
 */
@Singleton
class AuthTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_FILE_NAME = "auth_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to create encrypted prefs: ${e.message}")
            // Fallback to regular SharedPreferences (not recommended for production)
            context.getSharedPreferences("auth_fallback_prefs", Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Save authentication token securely
     */
    fun saveToken(token: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_AUTH_TOKEN, token)
                .apply()
            android.util.Log.d("AuthTokenStorage", "Token saved securely")
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to save token: ${e.message}")
        }
    }
    
    /**
     * Get saved authentication token
     */
    fun getToken(): String? {
        return try {
            val token = encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
            android.util.Log.d("AuthTokenStorage", "Token retrieved: ${if (token != null) "found" else "not found"}")
            token
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to retrieve token: ${e.message}")
            null
        }
    }
    
    /**
     * Save user information securely
     */
    fun saveUserInfo(userId: Long, email: String, name: String) {
        try {
            encryptedPrefs.edit().apply {
                putLong(KEY_USER_ID, userId)
                putString(KEY_USER_EMAIL, email)
                putString(KEY_USER_NAME, name)
            }.apply()
            android.util.Log.d("AuthTokenStorage", "User info saved for: $name ($email)")
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to save user info: ${e.message}")
        }
    }
    
    /**
     * Get saved user ID
     */
    fun getUserId(): Long? {
        return try {
            val userId = encryptedPrefs.getLong(KEY_USER_ID, -1)
            if (userId == -1L) null else userId
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to retrieve user ID: ${e.message}")
            null
        }
    }
    
    /**
     * Get saved user email
     */
    fun getUserEmail(): String? {
        return try {
            encryptedPrefs.getString(KEY_USER_EMAIL, null)
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to retrieve user email: ${e.message}")
            null
        }
    }
    
    /**
     * Get saved user name
     */
    fun getUserName(): String? {
        return try {
            encryptedPrefs.getString(KEY_USER_NAME, null)
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to retrieve user name: ${e.message}")
            null
        }
    }
    
    /**
     * Check if user data is stored (indicates previous login)
     */
    fun hasStoredCredentials(): Boolean {
        return getToken() != null && getUserId() != null
    }
    
    /**
     * Save token expiry timestamp
     */
    fun saveTokenExpiry(expiryTimestamp: Long) {
        try {
            encryptedPrefs.edit()
                .putLong(KEY_TOKEN_EXPIRY, expiryTimestamp)
                .apply()
            android.util.Log.d("AuthTokenStorage", "Token expiry saved: ${java.time.Instant.ofEpochMilli(expiryTimestamp)}")
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to save token expiry: ${e.message}")
        }
    }
    
    /**
     * Get saved token expiry timestamp
     */
    fun getTokenExpiry(): Long? {
        return try {
            val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, -1)
            if (expiry == -1L) null else expiry
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to retrieve token expiry: ${e.message}")
            null
        }
    }
    
    /**
     * Clear all stored authentication data
     */
    fun clearAll() {
        try {
            encryptedPrefs.edit().clear().apply()
            android.util.Log.d("AuthTokenStorage", "All stored auth data cleared")
        } catch (e: Exception) {
            android.util.Log.e("AuthTokenStorage", "Failed to clear stored data: ${e.message}")
        }
    }
    
    /**
     * Get stored user info as a data class
     */
    fun getStoredUserInfo(): StoredUserInfo? {
        val userId = getUserId()
        val email = getUserEmail()
        val name = getUserName()
        val token = getToken()
        
        return if (userId != null && !email.isNullOrEmpty() && !name.isNullOrEmpty() && !token.isNullOrEmpty()) {
            StoredUserInfo(userId, email, name, token)
        } else {
            null
        }
    }
}

/**
 * Data class for stored user information
 */
data class StoredUserInfo(
    val userId: Long,
    val email: String,
    val name: String,
    val token: String
)