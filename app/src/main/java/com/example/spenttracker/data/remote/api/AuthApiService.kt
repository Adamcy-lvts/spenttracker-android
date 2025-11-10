package com.example.spenttracker.data.remote.api

import com.example.spenttracker.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("v1/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<AuthResponse>
    
    @POST("v1/register")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): Response<AuthResponse>
    
    @POST("v1/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
    
    @POST("v1/refresh")
    suspend fun refreshToken(): Response<AuthResponse>
    
    @GET("v1/user")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>
    
    @GET("health")
    suspend fun healthCheck(): Response<Any>
}