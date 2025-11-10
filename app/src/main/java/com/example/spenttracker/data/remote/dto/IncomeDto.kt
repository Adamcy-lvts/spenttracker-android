package com.example.spenttracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Income API communication
 * Matches Laravel API response structure
 */
data class IncomeDto(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("source")
    val source: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("date")
    val date: String,

    @SerializedName("category_id")
    val categoryId: Long? = null,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("is_recurring")
    val isRecurring: Boolean = false,

    @SerializedName("recurrence_type")
    val recurrenceType: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    // Category information when included
    @SerializedName("category")
    val category: CategoryDto? = null
)

/**
 * Request DTO for creating income
 */
data class CreateIncomeRequest(
    @SerializedName("source")
    val source: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("date")
    val date: String,

    @SerializedName("category_id")
    val categoryId: Long? = null,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("is_recurring")
    val isRecurring: Boolean = false,

    @SerializedName("recurrence_type")
    val recurrenceType: String? = null
)

/**
 * Request DTO for updating income
 */
data class UpdateIncomeRequest(
    @SerializedName("source")
    val source: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("date")
    val date: String,

    @SerializedName("category_id")
    val categoryId: Long? = null,

    @SerializedName("description")
    val description: String = "",

    @SerializedName("is_recurring")
    val isRecurring: Boolean = false,

    @SerializedName("recurrence_type")
    val recurrenceType: String? = null
)

/**
 * Response wrapper for single income
 */
data class IncomeResponse(
    @SerializedName("data")
    val data: IncomeDto,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Response wrapper for income list
 */
data class IncomesResponse(
    @SerializedName("data")
    val data: List<IncomeDto>,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("pagination")
    val pagination: PaginationDto? = null
)
