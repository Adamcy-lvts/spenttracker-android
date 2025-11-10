package com.example.spenttracker.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.data.remote.api.AuthApiService
import com.example.spenttracker.data.remote.dto.LoginRequest
import com.example.spenttracker.data.remote.dto.RegisterRequest
import com.example.spenttracker.data.remote.auth.AuthTokenProviderImpl
import com.example.spenttracker.data.auth.UserContextProviderImpl
import com.example.spenttracker.data.auth.AuthTokenStorage
import com.example.spenttracker.data.auth.SessionManager
import com.example.spenttracker.data.auth.TokenManager
import com.example.spenttracker.data.auth.TokenRefreshManager
import com.example.spenttracker.data.sync.SyncScheduler
import com.example.spenttracker.data.repository.ExpenseRepositoryImpl
import com.example.spenttracker.data.mapper.toDomain
import com.example.spenttracker.domain.model.User
import com.example.spenttracker.util.NotificationScheduler
import com.example.spenttracker.data.preferences.SettingsManager
import com.example.spenttracker.util.LocationManager
import com.example.spenttracker.util.LocationData
import com.example.spenttracker.util.PhoneNumberValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Auth ViewModel - Like Laravel's AuthController
 * Handles login and registration logic with state management
 */

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isRegistered: Boolean = false,
    val currentUser: User? = null,
    val authToken: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val authTokenProvider: AuthTokenProviderImpl,
    private val userContextProvider: UserContextProviderImpl,
    private val expenseRepository: ExpenseRepositoryImpl,
    private val tokenStorage: AuthTokenStorage,
    private val sessionManager: SessionManager,
    private val tokenManager: TokenManager,
    private val tokenRefreshManager: TokenRefreshManager,
    private val syncScheduler: SyncScheduler,
    private val notificationScheduler: NotificationScheduler,
    private val settingsManager: SettingsManager,
    private val locationManager: LocationManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val _uiState = MutableStateFlow(AuthUiState())
    
    init {
        // Set up token refresh callbacks
        tokenRefreshManager.setOnRefreshSuccess { newToken ->
            Log.i(TAG, "Token refresh successful - session continues")
            // Token is already updated by TokenRefreshManager
        }

        tokenRefreshManager.setOnRefreshFailed { error ->
            Log.e(TAG, "Token refresh failed: $error - forcing logout")
            performAutoLogout()
        }

        // Check for existing credentials on startup
        checkStoredCredentials()
    }
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    /**
     * Login user - Like Laravel's login() method
     */
    fun login(login: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting login for: $login")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                
                // Validate input - like Laravel's validation
                if (login.isBlank() || password.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Please fill in all fields"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Network connectivity confirmed, proceeding with login...")
                
                // Call API - like Laravel's Auth::attempt()
                Log.d(TAG, "Calling API login endpoint")
                Log.d(TAG, "Creating LoginRequest with login: $login")
                
                // Get current location for login tracking
                Log.d(TAG, "Attempting to get current location...")
                val locationData = try {
                    if (locationManager.hasLocationPermission()) {
                        Log.d(TAG, "Location permission granted, fetching location...")
                        val location = locationManager.getCurrentLocation()
                        
                        // Enhanced logging for Places API debugging
                        location?.let { loc ->
                            Log.d(TAG, "=== LOCATION DATA RECEIVED ===")
                            Log.d(TAG, "Coordinates: ${loc.latitude}, ${loc.longitude}")
                            Log.d(TAG, "Display Location: ${loc.displayLocation}")
                            Log.d(TAG, "City: ${loc.city}")
                            Log.d(TAG, "Country: ${loc.country}")
                            Log.d(TAG, "Street Address: ${loc.streetAddress}")
                            Log.d(TAG, "Sub Locality: ${loc.subLocality}")
                            Log.d(TAG, "Admin Area: ${loc.adminArea}")
                            Log.d(TAG, "Postal Code: ${loc.postalCode}")
                            
                            // Places API specific data
                            Log.d(TAG, "=== PLACES API DATA ===")
                            Log.d(TAG, "Nearby Place Name: ${loc.nearbyPlaceName}")
                            Log.d(TAG, "Nearby Place Type: ${loc.nearbyPlaceType}")
                            Log.d(TAG, "Nearby Place Address: ${loc.nearbyPlaceAddress}")
                            
                            if (loc.nearbyPlaceName != null) {
                                Log.d(TAG, "✅ Places API SUCCESS: Found nearby place '${loc.nearbyPlaceName}'")
                            } else {
                                Log.w(TAG, "⚠️ Places API: No nearby places found")
                            }
                            Log.d(TAG, "=== END LOCATION DATA ===")
                        } ?: Log.w(TAG, "❌ Location data is null")
                        
                        location
                    } else {
                        Log.w(TAG, "Location permission not granted, skipping location capture")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get location for login: ${e.message}", e)
                    null
                }
                
                val loginRequest = LoginRequest(
                    login = login,
                    password = password,
                    latitude = locationData?.latitude,
                    longitude = locationData?.longitude,
                    city = locationData?.city,
                    country = locationData?.country,
                    streetAddress = locationData?.streetAddress,
                    neighborhood = locationData?.subLocality,
                    district = locationData?.subAdminArea,
                    state = locationData?.adminArea,
                    postalCode = locationData?.postalCode,
                    fullAddress = locationData?.displayLocation,
                    // Enhanced Places API data
                    nearbyPlaceName = locationData?.nearbyPlaceName,
                    nearbyPlaceType = locationData?.nearbyPlaceType,
                    nearbyPlaceAddress = locationData?.nearbyPlaceAddress
                )
                Log.d(TAG, "LoginRequest created successfully with location: ${locationData?.displayLocation}")
                
                val response = try {
                    Log.d(TAG, "About to call authApiService.login()...")
                    val apiResponse = authApiService.login(loginRequest)
                    Log.d(TAG, "authApiService.login() completed successfully")
                    apiResponse
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e(TAG, "Socket timeout: ${e.message}", e)
                    throw e
                } catch (e: java.net.ConnectException) {
                    Log.e(TAG, "Connection failed: ${e.message}", e)
                    throw e
                } catch (e: java.net.UnknownHostException) {
                    Log.e(TAG, "Unknown host: ${e.message}", e)
                    throw e
                } catch (e: retrofit2.HttpException) {
                    Log.e(TAG, "HTTP error: ${e.code()} - ${e.message()}", e)
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "API call failed with exception: ${e.message}", e)
                    Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "Exception stack trace:", e)
                    throw e
                }
                
                Log.d(TAG, "Received API response: ${response.code()} - ${response.message()}")
                Log.d(TAG, "Response body exists: ${response.body() != null}")
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Store user data and token
                        val user = authResponse.user.toDomain()
                        Log.d(TAG, "Login successful! User: ${user.name} (${user.email}), Token: ${authResponse.token}")
                        
                        // Get the actual token (prefer access_token if available)
                        val actualToken = authResponse.accessToken ?: authResponse.token
                        
                        // Update token provider for API requests
                        authTokenProvider.updateToken(actualToken)
                        
                        // Store token with expiry information for refresh management
                        tokenManager.storeTokenWithExpiry(
                            token = actualToken,
                            expiresIn = authResponse.expiresIn?.toLong(),
                            expiresAt = authResponse.expiresAt
                        )
                        
                        // Save user information to persistent storage
                        tokenStorage.saveUserInfo(user.id, user.email, user.name)
                        
                        // Update user context for data filtering
                        userContextProvider.updateUserId(user.id)
                        
                        // Migrate any orphaned expenses to this user
                        val migratedCount = expenseRepository.migrateOrphanedExpensesToCurrentUser()
                        Log.d(TAG, "Login migration: $migratedCount orphaned expenses assigned to user ${user.id}")
                        
                        // Start session management
                        sessionManager.startSession()
                        
                        // Start background sync scheduling
                        syncScheduler.startSyncScheduling()
                        
                        // Schedule notifications based on user settings
                        scheduleNotificationsFromSettings()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user,
                            authToken = authResponse.token,
                            successMessage = "Welcome back, ${user.name}!"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Login failed: No data received"
                        )
                    }
                } else {
                    // Handle API errors - like Laravel's validation errors
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid credentials"
                        422 -> "Please check your input and try again"
                        500 -> "Server error. Please try again later"
                        else -> "Login failed: ${response.message()}"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
                
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Login timeout: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Request timed out. Please check your internet connection."
                )
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "Unknown host: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Cannot reach server. Please check your internet connection."
                )
            } catch (e: javax.net.ssl.SSLException) {
                Log.e(TAG, "SSL error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "SSL connection error: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Login exception: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Register user - Like Laravel's register() method
     */
    fun register(name: String, email: String, phoneNumber: String, password: String, confirmPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
                
                // Validate input - like Laravel's validation rules
                val validationError = validateRegistrationInput(name, email, phoneNumber, password, confirmPassword)
                if (validationError != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = validationError
                    )
                    return@launch
                }
                
                // Get current location for registration tracking
                Log.d(TAG, "Attempting to get current location for registration...")
                val locationData = try {
                    if (locationManager.hasLocationPermission()) {
                        Log.d(TAG, "Location permission granted, fetching location...")
                        val location = locationManager.getCurrentLocation()
                        Log.d(TAG, "Location result: $location")
                        location
                    } else {
                        Log.w(TAG, "Location permission not granted, skipping location capture")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get location for registration: ${e.message}", e)
                    null
                }
                
                // Call API - like Laravel's User::create()
                val response = authApiService.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber.takeIf { it.isNotBlank() },
                        password = password,
                        passwordConfirmation = confirmPassword,
                        latitude = locationData?.latitude,
                        longitude = locationData?.longitude,
                        city = locationData?.city,
                        country = locationData?.country,
                        streetAddress = locationData?.streetAddress,
                        neighborhood = locationData?.subLocality,
                        district = locationData?.subAdminArea,
                        state = locationData?.adminArea,
                        postalCode = locationData?.postalCode,
                        fullAddress = locationData?.displayLocation
                    )
                )
                Log.d(TAG, "Registration request sent with location: ${locationData?.displayLocation}")
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Store user data and token 
                        val user = authResponse.user.toDomain()
                        Log.d(TAG, "Registration successful! User: ${user.name} (${user.email}), Token: ${authResponse.token}")
                        
                        // Get the actual token (prefer access_token if available)
                        val actualToken = authResponse.accessToken ?: authResponse.token
                        
                        // Update token provider for API requests
                        authTokenProvider.updateToken(actualToken)
                        
                        // Store token with expiry information for refresh management
                        tokenManager.storeTokenWithExpiry(
                            token = actualToken,
                            expiresIn = authResponse.expiresIn?.toLong(),
                            expiresAt = authResponse.expiresAt
                        )
                        
                        // Save user information to persistent storage
                        tokenStorage.saveUserInfo(user.id, user.email, user.name)
                        
                        // Update user context for data filtering
                        userContextProvider.updateUserId(user.id)
                        
                        // Migrate any orphaned expenses to this user
                        val migratedCount = expenseRepository.migrateOrphanedExpensesToCurrentUser()
                        Log.d(TAG, "Registration migration: $migratedCount orphaned expenses assigned to user ${user.id}")
                        
                        // Start session management
                        sessionManager.startSession()
                        
                        // Start background sync scheduling
                        syncScheduler.startSyncScheduling()
                        
                        // Schedule notifications based on user settings
                        scheduleNotificationsFromSettings()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRegistered = true,
                            isLoggedIn = true,
                            currentUser = user,
                            authToken = authResponse.token,
                            successMessage = "Welcome, ${user.name}! Account created successfully!"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Registration failed: No data received"
                        )
                    }
                } else {
                    // Handle API errors - like Laravel's validation errors
                    val errorMsg = when (response.code()) {
                        422 -> "Email already exists or validation failed"
                        500 -> "Server error. Please try again later"
                        else -> "Registration failed: ${response.message()}"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Network error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Logout user - Enhanced version that preserves offline access option
     */
    fun logout(preserveOfflineAccess: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== LOGOUT PROCESS ===")
                Log.d(TAG, "Preserve offline access: $preserveOfflineAccess")
                
                // Call API logout - like Laravel's auth()->logout()
                authApiService.logout()
                Log.d(TAG, "API logout successful")
            } catch (e: Exception) {
                Log.w(TAG, "API logout failed: ${e.message} - continuing with local logout")
                // Continue with local logout even if API call fails
            } finally {
                // Stop token monitoring and refresh
                tokenManager.stopMonitoring()
                tokenRefreshManager.stopRefresh()
                
                // End session management
                sessionManager.endSession()
                
                // Stop background sync scheduling
                syncScheduler.stopSyncScheduling()
                
                if (preserveOfflineAccess) {
                    // Clear API token but preserve user session for offline access
                    Log.i(TAG, "🔒 OFFLINE MODE: Preserving session for offline access")
                    authTokenProvider.updateToken(null) // Clear API token
                    // Keep userContextProvider.updateUserId unchanged for offline access
                    
                    _uiState.value = _uiState.value.copy(
                        authToken = null, // Clear API token
                        successMessage = "Logged out from server. Offline access preserved."
                        // Keep isLoggedIn = true and currentUser for offline access
                    )
                } else {
                    // Complete logout - clear everything
                    Log.d(TAG, "🚪 COMPLETE LOGOUT: Clearing all session data")
                    authTokenProvider.updateToken(null)
                    userContextProvider.clearCurrentSession() // Use new method that preserves stored data
                    
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = false,
                        isRegistered = false,
                        currentUser = null,
                        authToken = null,
                        successMessage = "Logged out successfully"
                    )
                }
                
                Log.d(TAG, "Logout completed successfully")
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Clear success message
     */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Check for stored credentials and restore session for offline access
     */
    private fun checkStoredCredentials() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== OFFLINE ACCESS RESTORATION ===")
                Log.d(TAG, "Checking for stored credentials and session...")
                
                // First, try to restore session from UserContextProvider (for offline access)
                userContextProvider.restoreStoredSession()
                
                // Wait for session restoration to complete
                userContextProvider.isSessionRestored.collect { isRestored ->
                    if (isRestored) {
                        val currentUserId = userContextProvider.getCurrentUserId()
                        
                        if (currentUserId != null) {
                            Log.i(TAG, "✅ OFFLINE ACCESS ENABLED: Session restored for user ID: $currentUserId")
                            
                            // Try to get additional user info from token storage
                            val storedUserInfo = tokenStorage.getStoredUserInfo()
                            
                            val user = if (storedUserInfo != null && storedUserInfo.userId == currentUserId) {
                                Log.d(TAG, "Found complete user info: ${storedUserInfo.name} (${storedUserInfo.email})")
                                com.example.spenttracker.domain.model.User(
                                    id = storedUserInfo.userId,
                                    name = storedUserInfo.name,
                                    email = storedUserInfo.email
                                )
                            } else {
                                // Create minimal user object for offline access
                                Log.d(TAG, "Using minimal user info for offline access")
                                com.example.spenttracker.domain.model.User(
                                    id = currentUserId,
                                    name = "User",
                                    email = "offline@user.com"
                                )
                            }
                            
                            // Update UI to show logged in state (enables offline access)
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                currentUser = user,
                                authToken = storedUserInfo?.token
                            )
                            
                            // Start session management for restored session
                            sessionManager.startSession()
                            
                            // Start background sync scheduling (will queue for when online)
                            syncScheduler.startSyncScheduling()
                            
                            // Schedule notifications based on user settings
                            scheduleNotificationsFromSettings()
                            
                            Log.i(TAG, "🎉 OFFLINE ACCESS READY: User can access local data without network!")
                            
                        } else {
                            Log.d(TAG, "❌ No stored session found - user needs to login")
                            // Check if we have token storage data for online login attempt
                            val storedUserInfo = tokenStorage.getStoredUserInfo()
                            if (storedUserInfo != null) {
                                Log.d(TAG, "Found token storage data - attempting token validation...")
                                attemptTokenBasedLogin(storedUserInfo)
                            }
                        }
                        
                        // Break out of collect after first emission
                        return@collect
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during session restoration: ${e.message}", e)
                // Even if restoration fails, still check token storage for fallback
                try {
                    val storedUserInfo = tokenStorage.getStoredUserInfo()
                    if (storedUserInfo != null) {
                        attemptTokenBasedLogin(storedUserInfo)
                    }
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fallback token check failed: ${fallbackException.message}", fallbackException)
                }
            }
        }
    }
    
    /**
     * Attempt login using stored token (requires network)
     */
    private suspend fun attemptTokenBasedLogin(storedUserInfo: com.example.spenttracker.data.auth.StoredUserInfo) {
        try {
            Log.d(TAG, "Attempting token-based login for: ${storedUserInfo.name}")
            
            // Validate token is still valid (requires network)
            if (storedUserInfo.token.isNotEmpty()) {
                // Update token provider
                authTokenProvider.updateToken(storedUserInfo.token)
                
                // Try a simple API call to validate token
                val user = com.example.spenttracker.domain.model.User(
                    id = storedUserInfo.userId,
                    name = storedUserInfo.name,
                    email = storedUserInfo.email
                )
                
                // Update user context
                userContextProvider.updateUserId(storedUserInfo.userId)
                
                // Start session management
                sessionManager.startSession()
                
                // Start background sync scheduling
                syncScheduler.startSyncScheduling()
                
                // Schedule notifications
                scheduleNotificationsFromSettings()
                
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    currentUser = user,
                    authToken = storedUserInfo.token
                )
                
                Log.d(TAG, "Token-based login successful for user: ${user.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Token-based login failed: ${e.message} - user will need to login manually")
        }
    }
    
    /**
     * Perform auto-logout when token refresh fails
     */
    private fun performAutoLogout() {
        viewModelScope.launch {
            Log.w(TAG, "Performing auto-logout due to token refresh failure")

            // Clear stored data but don't call API logout (token already invalid)
            sessionManager.endSession()
            authTokenProvider.updateToken(null)
            userContextProvider.updateUserId(null)

            // Cancel all expense reminder notifications
            notificationScheduler.cancelAllReminders()

            _uiState.value = _uiState.value.copy(
                isLoggedIn = false,
                isRegistered = false,
                currentUser = null,
                authToken = null,
                errorMessage = "Session expired. Please log in again."
            )
        }
    }

    /**
     * Update user activity - no longer affects session (kept for compatibility)
     * @deprecated Sessions no longer expire automatically
     */
    @Deprecated("Sessions no longer expire automatically")
    fun updateUserActivity() {
        // No-op: Sessions persist until manual logout
    }

    /**
     * Get remaining session time - always returns Long.MAX_VALUE
     * @deprecated Sessions no longer expire automatically
     */
    @Deprecated("Sessions no longer expire automatically")
    fun getRemainingSessionTime(): Long {
        return Long.MAX_VALUE
    }

    /**
     * Check if session is about to expire - always returns false
     * @deprecated Sessions no longer expire automatically
     */
    @Deprecated("Sessions no longer expire automatically")
    fun isSessionAboutToExpire(): Boolean {
        return false
    }

    /**
     * Extend current session - no longer needed
     * @deprecated Sessions no longer expire automatically
     */
    @Deprecated("Sessions no longer expire automatically")
    fun extendSession() {
        // No-op: Sessions don't expire
    }

    /**
     * Configure session timeout - no longer used
     * @deprecated Sessions no longer expire automatically
     */
    @Deprecated("Sessions no longer expire automatically")
    fun setSessionTimeout(minutes: Int) {
        Log.w(TAG, "setSessionTimeout called but sessions no longer expire automatically")
    }
    
    /**
     * Schedule notifications based on current user settings
     */
    private fun scheduleNotificationsFromSettings() {
        viewModelScope.launch {
            try {
                val appSettings = settingsManager.appSettings.value
                
                if (appSettings.notificationsEnabled) {
                    val enabledReminders = appSettings.reminders.filter { it.isEnabled }
                    
                    if (enabledReminders.isNotEmpty()) {
                        // Schedule multiple reminders based on settings
                        notificationScheduler.scheduleMultipleReminders(enabledReminders)
                        Log.d(TAG, "Scheduled ${enabledReminders.size} reminders from settings")
                    } else {
                        // No enabled reminders, use default if none exist
                        if (appSettings.reminders.isEmpty()) {
                            notificationScheduler.scheduleDailyExpenseReminder()
                            Log.d(TAG, "Scheduled default reminder (no custom reminders)")
                        } else {
                            Log.d(TAG, "No enabled reminders, not scheduling any notifications")
                        }
                    }
                } else {
                    Log.d(TAG, "Notifications disabled in settings")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling notifications from settings", e)
                // Fallback to default scheduling if settings fail
                notificationScheduler.scheduleDailyExpenseReminder()
            }
        }
    }
    
    /**
     * Validate registration input - Like Laravel's validation rules
     */
    private fun validateRegistrationInput(
        name: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            email.isBlank() && phoneNumber.isBlank() -> "Either email or phone number must be provided"
            email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid email address"
            phoneNumber.isNotBlank() && !PhoneNumberValidator.isValidNigerianPhoneNumber(phoneNumber) -> 
                PhoneNumberValidator.getValidationErrorMessage(phoneNumber) ?: "Please enter a valid phone number"
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            confirmPassword.isBlank() -> "Please confirm your password"
            password != confirmPassword -> "Passwords do not match"
            else -> null
        }
    }
}