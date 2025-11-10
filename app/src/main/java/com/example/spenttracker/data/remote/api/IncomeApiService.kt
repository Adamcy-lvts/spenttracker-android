package com.example.spenttracker.data.remote.api

import com.example.spenttracker.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service for Income endpoints
 * Defines API endpoints matching Laravel backend structure
 */
interface IncomeApiService {

    /**
     * Get all incomes for sync (matches Laravel: GET /api/v1/incomes/sync)
     */
    @GET("v1/incomes/sync")
    suspend fun getAllIncomes(): Response<IncomesResponse>

    /**
     * Get paginated incomes (matches Laravel: GET /api/v1/incomes?page=1&per_page=15)
     */
    @GET("v1/incomes")
    suspend fun getIncomes(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15
    ): Response<IncomesResponse>

    /**
     * Get single income (matches Laravel: GET /api/v1/incomes/{id})
     */
    @GET("v1/incomes/{id}")
    suspend fun getIncome(
        @Path("id") id: Long
    ): Response<IncomeResponse>

    /**
     * Create new income (matches Laravel: POST /api/v1/incomes)
     */
    @POST("v1/incomes")
    suspend fun createIncome(
        @Body request: CreateIncomeRequest
    ): Response<IncomeResponse>

    /**
     * Update income (matches Laravel: PUT /api/v1/incomes/{id})
     */
    @PUT("v1/incomes/{id}")
    suspend fun updateIncome(
        @Path("id") id: Long,
        @Body request: UpdateIncomeRequest
    ): Response<IncomeResponse>

    /**
     * Delete income (matches Laravel: DELETE /api/v1/incomes/{id})
     */
    @DELETE("v1/incomes/{id}")
    suspend fun deleteIncome(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>

    /**
     * Bulk delete incomes (matches Laravel: POST /api/v1/incomes/bulk-delete)
     */
    @POST("v1/incomes/bulk-delete")
    suspend fun bulkDeleteIncomes(
        @Body request: BulkDeleteRequest
    ): Response<ApiResponse<Unit>>
}
