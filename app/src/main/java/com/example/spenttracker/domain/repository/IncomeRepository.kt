package com.example.spenttracker.domain.repository

import com.example.spenttracker.domain.model.Income
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Repository interface for Income operations
 * Defines the contract for income data management
 */
interface IncomeRepository {

    /**
     * Get all incomes for the current user
     */
    fun getAllIncomes(): Flow<List<Income>>

    /**
     * Get incomes for a specific month
     */
    fun getIncomesForMonth(yearMonth: YearMonth): Flow<List<Income>>

    /**
     * Get total income for a specific month
     */
    suspend fun getTotalIncomeForMonth(yearMonth: YearMonth): Double

    /**
     * Get recurring incomes
     */
    fun getRecurringIncomes(): Flow<List<Income>>

    /**
     * Get incomes by source
     */
    fun getIncomesBySource(source: String): Flow<List<Income>>

    /**
     * Get all unique income sources
     */
    fun getIncomeSources(): Flow<List<String>>

    /**
     * Add a new income
     */
    suspend fun addIncome(income: Income): Result<Income>

    /**
     * Update an existing income
     */
    suspend fun updateIncome(income: Income): Result<Income>

    /**
     * Delete an income by ID
     */
    suspend fun deleteIncome(incomeId: Int): Result<Unit>

    /**
     * Sync incomes with remote server
     */
    suspend fun syncIncomes(): Result<Unit>

    /**
     * Get income count
     */
    suspend fun getIncomeCount(): Int
}
