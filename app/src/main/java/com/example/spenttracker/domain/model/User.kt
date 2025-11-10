package com.example.spenttracker.domain.model

/**
 * User domain model - Like Laravel's User model
 * Represents the authenticated user
 */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val emailVerifiedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val isAdmin: Boolean = false,
    val lastLoginAt: String? = null,
    val lastLoginIp: String? = null,
    val lastLoginUserAgent: String? = null,
    val lastLoginLocation: String? = null,
    val lastLoginLatitude: Double? = null,
    val lastLoginLongitude: Double? = null,
    val lastLoginCity: String? = null,
    val lastLoginCountry: String? = null,
    val lastLoginDeviceType: String? = null,
    val phoneNumber: String? = null
)