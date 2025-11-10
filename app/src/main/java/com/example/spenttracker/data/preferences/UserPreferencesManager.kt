package com.example.spenttracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences manager using DataStore - Like Laravel's session/cache
 * Handles secure storage of authentication tokens and user preferences
 * 
 * Kotlin Syntax Explanations:
 * - DataStore: Modern replacement for SharedPreferences (like Laravel's cache)
 * - Flow<T>: Reactive data stream (like Laravel's real-time events)
 * - suspend fun: Async function (like Laravel's async operations)
 * - private val Context.dataStore: Extension property (like Laravel's macros)
 */

// Create DataStore instance - like Laravel's cache store configuration
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesManager @Inject constructor(
    private val context: Context
) {
    
    // Preference keys - like Laravel's cache keys
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id") 
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val BASE_URL_KEY = stringPreferencesKey("base_url")
    }
    
    /**
     * Save authentication data - Like Laravel's session store
     * 
     * @param token Bearer token from Laravel API
     * @param userId User ID from Laravel
     * @param userName User name 
     * @param userEmail User email
     */
    suspend fun saveAuthData(
        token: String,
        userId: String,
        userName: String,
        userEmail: String
    ) {
        context.dataStore.edit { preferences ->
            // Like Laravel: session(['auth_token' => $token, ...])
            preferences[AUTH_TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = userName
            preferences[USER_EMAIL_KEY] = userEmail
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }
    
    /**
     * Get authentication token - Like Laravel's session('auth_token')
     */
    fun getAuthToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }
    }
    
    /**
     * Get current auth token synchronously (for interceptor)
     * Like Laravel's session()->get('auth_token')
     */
    suspend fun getCurrentAuthToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }.let { flow ->
            // Get current value from flow
            var token: String? = null
            flow.collect { token = it }
            token
        }
    }
    
    /**
     * Get user data - Like Laravel's auth()->user()
     */
    fun getUserData(): Flow<UserData> {
        return context.dataStore.data.map { preferences ->
            UserData(
                userId = preferences[USER_ID_KEY] ?: "",
                userName = preferences[USER_NAME_KEY] ?: "",
                userEmail = preferences[USER_EMAIL_KEY] ?: "",
                isLoggedIn = preferences[IS_LOGGED_IN_KEY] ?: false
            )
        }
    }
    
    /**
     * Check if user is logged in - Like Laravel's auth()->check()
     */
    fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN_KEY] ?: false
        }
    }
    
    /**
     * Logout user - Like Laravel's auth()->logout()
     */
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            // Clear all auth data - like Laravel's session()->flush()
            preferences.remove(AUTH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_NAME_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences[IS_LOGGED_IN_KEY] = false
        }
    }
    
    /**
     * Save API base URL - Like Laravel's config('app.url')
     */
    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = url
        }
    }
    
    /**
     * Get API base URL - Like Laravel's config('app.url')
     */
    fun getBaseUrl(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[BASE_URL_KEY] ?: "https://spentracker.live/api/"  // Your live domain
        }
    }
}

/**
 * User data class - Like Laravel's User model data
 * 
 * Kotlin Syntax Explanation:
 * - data class: Auto-generates equals, hashCode, toString (like Laravel's Eloquent)
 * - Immutable properties for thread safety
 */
data class UserData(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val isLoggedIn: Boolean
)