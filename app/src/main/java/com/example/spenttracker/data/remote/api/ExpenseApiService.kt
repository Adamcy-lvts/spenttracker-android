package com.example.spenttracker.data.remote.api

import com.example.spenttracker.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ExpenseApiService {
    
    /**
     * Get all expenses for sync (matches Laravel: GET /api/v1/expenses/sync)
     */
    @GET("v1/expenses/sync")
    suspend fun getAllExpenses(): Response<ExpensesApiResponse>
    
    /**
     * Get paginated expenses (matches Laravel: GET /api/v1/expenses?page=1&per_page=15)
     */
    @GET("v1/expenses")
    suspend fun getExpenses(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15
    ): Response<ExpensesApiResponse>
    
    /**
     * Get single expense (matches Laravel: GET /api/v1/expenses/{id})
     */
    @GET("v1/expenses/{id}")
    suspend fun getExpense(
        @Path("id") id: Long
    ): Response<ExpenseApiResponse>
    
    /**
     * Create new expense (matches Laravel: POST /api/v1/expenses)
     */
    @POST("v1/expenses")
    suspend fun createExpense(
        @Body request: CreateExpenseRequest
    ): Response<ExpenseApiResponse>
    
    /**
     * Update expense (matches Laravel: PUT /api/v1/expenses/{id})
     */
    @PUT("v1/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: Long,
        @Body request: UpdateExpenseRequest
    ): Response<ExpenseApiResponse>
    
    /**
     * Delete expense (matches Laravel: DELETE /api/v1/expenses/{id})
     */
    @DELETE("v1/expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
    
    /**
     * Bulk delete expenses (matches Laravel: POST /api/v1/expenses/bulk-delete)
     */
    @POST("v1/expenses/bulk-delete")
    suspend fun bulkDeleteExpenses(
        @Body request: BulkDeleteRequest
    ): Response<ApiResponse<Unit>>
}