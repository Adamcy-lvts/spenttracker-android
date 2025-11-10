package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.remote.dto.UserDto
import com.example.spenttracker.domain.model.User

/**
 * User mapper - Like Laravel's UserResource
 * Converts between DTO and Domain models
 */

fun UserDto.toDomain(): User {
    return User(
        id = this.id,
        name = this.name,
        email = this.email,
        emailVerifiedAt = null, // Not included in current DTO
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isAdmin = this.isAdmin,
        lastLoginAt = this.lastLoginAt,
        lastLoginIp = this.lastLoginIp,
        lastLoginUserAgent = this.lastLoginUserAgent,
        lastLoginLocation = this.lastLoginLocation,
        lastLoginLatitude = this.lastLoginLatitude,
        lastLoginLongitude = this.lastLoginLongitude,
        lastLoginCity = this.lastLoginCity,
        lastLoginCountry = this.lastLoginCountry,
        lastLoginDeviceType = this.lastLoginDeviceType,
        phoneNumber = this.phoneNumber
    )
}