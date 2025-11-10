package com.example.spenttracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("login")
    val login: String,
    
    @SerializedName("password")
    val password: String,
    
    // Location data for login tracking
    @SerializedName("latitude")
    val latitude: Double? = null,
    
    @SerializedName("longitude")
    val longitude: Double? = null,
    
    @SerializedName("city")
    val city: String? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    // Detailed location data for precise tracking
    @SerializedName("street_address")
    val streetAddress: String? = null,
    
    @SerializedName("neighborhood")
    val neighborhood: String? = null,
    
    @SerializedName("district")
    val district: String? = null,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("postal_code")
    val postalCode: String? = null,
    
    @SerializedName("full_address")
    val fullAddress: String? = null,
    
    // Enhanced Places API data
    @SerializedName("nearby_place_name")
    val nearbyPlaceName: String? = null,
    
    @SerializedName("nearby_place_type")
    val nearbyPlaceType: String? = null,
    
    @SerializedName("nearby_place_address")
    val nearbyPlaceAddress: String? = null
)

data class RegisterRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    
    @SerializedName("password_confirmation")
    val passwordConfirmation: String,
    
    // Location data for registration tracking
    @SerializedName("latitude")
    val latitude: Double? = null,
    
    @SerializedName("longitude")
    val longitude: Double? = null,
    
    @SerializedName("city")
    val city: String? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    // Detailed location data for precise tracking
    @SerializedName("street_address")
    val streetAddress: String? = null,
    
    @SerializedName("neighborhood")
    val neighborhood: String? = null,
    
    @SerializedName("district")
    val district: String? = null,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("postal_code")
    val postalCode: String? = null,
    
    @SerializedName("full_address")
    val fullAddress: String? = null
)

data class AuthResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("user")
    val user: UserDto,
    
    @SerializedName("token")
    val token: String,
    
    // New fields for token expiry (optional for backward compatibility)
    @SerializedName("access_token")
    val accessToken: String? = null,
    
    @SerializedName("expires_in")
    val expiresIn: Double? = null,  // seconds until expiry (can be decimal)
    
    @SerializedName("expires_at")  
    val expiresAt: String? = null  // ISO timestamp
)

data class UserDto(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String,
    
    @SerializedName("is_admin")
    val isAdmin: Boolean = false,
    
    @SerializedName("last_login_at")
    val lastLoginAt: String? = null,
    
    @SerializedName("last_login_ip")
    val lastLoginIp: String? = null,
    
    @SerializedName("last_login_user_agent")
    val lastLoginUserAgent: String? = null,
    
    @SerializedName("last_login_location")
    val lastLoginLocation: String? = null,
    
    @SerializedName("last_login_latitude")
    val lastLoginLatitude: Double? = null,
    
    @SerializedName("last_login_longitude")
    val lastLoginLongitude: Double? = null,
    
    @SerializedName("last_login_city")
    val lastLoginCity: String? = null,
    
    @SerializedName("last_login_country")
    val lastLoginCountry: String? = null,
    
    @SerializedName("last_login_device_type")
    val lastLoginDeviceType: String? = null,
    
    @SerializedName("phone_number")
    val phoneNumber: String? = null
)