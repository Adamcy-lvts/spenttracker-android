package com.example.spenttracker.data.remote.auth

/**
 * Auth Token Provider - Like Laravel's auth()->user()->api_token
 * Provides current authentication token for API requests
 */
interface AuthTokenProvider {
    fun getAuthToken(): String?
}