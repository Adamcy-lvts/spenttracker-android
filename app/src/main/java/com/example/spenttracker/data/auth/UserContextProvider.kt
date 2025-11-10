package com.example.spenttracker.data.auth

/**
 * User Context Provider - Like Laravel's auth()->id()
 * Provides current authenticated user ID for data filtering
 */
interface UserContextProvider {
    fun getCurrentUserId(): Long?
}