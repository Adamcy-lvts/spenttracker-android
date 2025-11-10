package com.example.spenttracker.data.remote.api

import com.example.spenttracker.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Category API Service - Communicates with Laravel CategoryController
 * Matches the Laravel routes: /api/v1/categories
 */
interface CategoryApiService {
    
    /**
     * Get all categories (matches Laravel: GET /api/v1/categories)
     */
    @GET("v1/categories")
    suspend fun getAllCategories(): Response<CategoriesApiResponse>
    
    /**
     * Get single category (matches Laravel: GET /api/v1/categories/{id})
     */
    @GET("v1/categories/{id}")
    suspend fun getCategory(
        @Path("id") id: Long
    ): Response<CategoryApiResponse>
    
    /**
     * Create new category (matches Laravel: POST /api/v1/categories)
     */
    @POST("v1/categories")
    suspend fun createCategory(
        @Body request: CreateCategoryRequest
    ): Response<CategoryApiResponse>
    
    /**
     * Update category (matches Laravel: PUT /api/v1/categories/{id})
     */
    @PUT("v1/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: UpdateCategoryRequest
    ): Response<CategoryApiResponse>
    
    /**
     * Delete category (matches Laravel: DELETE /api/v1/categories/{id})
     */
    @DELETE("v1/categories/{id}")
    suspend fun deleteCategory(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
}