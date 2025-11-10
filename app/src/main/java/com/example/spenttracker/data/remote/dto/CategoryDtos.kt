package com.example.spenttracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Category DTOs for API communication with Laravel backend
 */

/**
 * Category DTO matching Laravel Category model
 */
data class CategoryDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val color: String,
    val icon: String? = null,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Create Category Request DTO
 */
data class CreateCategoryRequest(
    val name: String,
    val color: String,
    val icon: String? = null
)

/**
 * Update Category Request DTO
 */
data class UpdateCategoryRequest(
    val name: String,
    val color: String,
    val icon: String? = null
)

/**
 * Single Category API Response
 */
data class CategoryApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: CategoryDto
)

/**
 * Multiple Categories API Response
 */
data class CategoriesApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<CategoryDto>
)