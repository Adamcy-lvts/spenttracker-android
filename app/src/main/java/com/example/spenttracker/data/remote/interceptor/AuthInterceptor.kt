package com.example.spenttracker.data.remote.interceptor

import android.util.Log
import com.example.spenttracker.data.auth.TokenManager
import com.example.spenttracker.data.auth.TokenRefreshManager
import com.example.spenttracker.data.remote.auth.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Auth Interceptor - Automatically handles token refresh on 401 responses
 * 
 * Like Laravel's auth middleware but with client-side token refresh capability:
 * 1. Adds Bearer token to all API requests
 * 2. Detects 401 Unauthorized responses (expired token)
 * 3. Automatically refreshes token and retries request
 * 4. Provides seamless user experience during token expiry
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
    private val tokenManager: TokenManager,
    private val tokenRefreshManager: TokenRefreshManager
) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for login/register endpoints
        if (isAuthEndpoint(originalRequest)) {
            return chain.proceed(originalRequest)
        }
        
        // Add auth token to request if available
        val requestWithAuth = addAuthToken(originalRequest)
        
        // Execute request
        val response = chain.proceed(requestWithAuth)
        
        // Handle 401 Unauthorized response (token expired)
        if (response.code == 401 && !isRefreshRequest(originalRequest)) {
            Log.w(TAG, "Received 401 Unauthorized - attempting token refresh")
            
            // Close the original response to avoid leaking connections
            response.close()
            
            // Attempt token refresh
            val refreshSuccess = attemptTokenRefresh()
            
            if (refreshSuccess) {
                Log.i(TAG, "Token refresh successful - retrying original request")
                
                // Retry original request with new token
                val newRequestWithAuth = addAuthToken(originalRequest)
                return chain.proceed(newRequestWithAuth)
                
            } else {
                Log.e(TAG, "Token refresh failed - letting 401 pass through")
                
                // Return a 401 response to trigger logout
                return Response.Builder()
                    .request(originalRequest)
                    .protocol(response.protocol)
                    .code(401)
                    .message("Unauthorized")
                    .body(null)
                    .build()
            }
        }
        
        return response
    }
    
    /**
     * Add authentication token to request headers
     */
    private fun addAuthToken(request: Request): Request {
        val token = tokenProvider.getAuthToken()
        
        return if (token != null) {
            request.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        } else {
            request
        }
    }
    
    /**
     * Check if this is an authentication endpoint (login/register)
     */
    private fun isAuthEndpoint(request: Request): Boolean {
        val url = request.url.toString()
        return url.contains("/login") || url.contains("/register")
    }
    
    /**
     * Check if this is a token refresh request (to avoid infinite loops)
     */
    private fun isRefreshRequest(request: Request): Boolean {
        return request.url.encodedPath.contains("/refresh")
    }
    
    /**
     * Attempt to refresh the token synchronously
     */
    private fun attemptTokenRefresh(): Boolean {
        return try {
            // Only attempt refresh if token is actually expired or expiring soon
            if (tokenManager.isTokenExpired() || tokenManager.isTokenExpiringSoon()) {
                Log.d(TAG, "Token needs refresh - initiating refresh process")
                
                // Use runBlocking to make the async refresh synchronous for the interceptor
                runBlocking {
                    var refreshCompleted = false
                    var refreshSuccess = false
                    
                    // Set up one-time callbacks for this refresh attempt
                    tokenRefreshManager.setOnRefreshSuccess { _ ->
                        refreshSuccess = true
                        refreshCompleted = true
                    }
                    
                    tokenRefreshManager.setOnRefreshFailed { _ ->
                        refreshSuccess = false
                        refreshCompleted = true
                    }
                    
                    // Start the refresh process
                    tokenRefreshManager.attemptTokenRefresh()
                    
                    // Wait for refresh to complete (with timeout)
                    val startTime = System.currentTimeMillis()
                    val timeoutMs = 30000L // 30 seconds timeout
                    
                    while (!refreshCompleted && (System.currentTimeMillis() - startTime) < timeoutMs) {
                        kotlinx.coroutines.delay(100) // Check every 100ms
                    }
                    
                    refreshCompleted && refreshSuccess
                }
            } else {
                Log.d(TAG, "Token doesn't need refresh yet")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh: ${e.message}", e)
            false
        }
    }
}