package com.example.spenttracker.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpenseDto(
    @SerializedName("id")
    val id: Long = 0,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("category_id")
    val categoryId: Long? = null,
    
    @SerializedName("user_id")
    val userId: Long,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    
    // Category information when included
    @SerializedName("category")
    val category: CategoryDto? = null
)

data class CreateExpenseRequest(
    @SerializedName("description")
    val description: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("category_id")
    val categoryId: Long? = null
)

data class UpdateExpenseRequest(
    @SerializedName("description")
    val description: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("category_id")
    val categoryId: Long? = null
)


// API Response wrappers to match Laravel structure
data class ExpenseApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("data")
    val data: ExpenseDto
)

data class ExpensesApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: List<ExpenseDto>,
    
    @SerializedName("pagination")
    val pagination: PaginationDto? = null
)

data class PaginationDto(
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("last_page")
    val lastPage: Int,
    
    @SerializedName("per_page")
    val perPage: Int,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("has_more")
    val hasMore: Boolean
)

data class BulkDeleteRequest(
    @SerializedName("expense_ids")
    val expenseIds: List<Long>
)