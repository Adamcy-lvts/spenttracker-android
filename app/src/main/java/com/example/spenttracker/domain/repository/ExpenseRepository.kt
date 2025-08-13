package com.example.spenttracker.domain.repository

import com.example.spenttracker.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for expense operations
 * Defines what operations are available, but not how they're implemented
 */
interface ExpenseRepository {
    
    /**
     * Get all expenses as a live stream
     * Returns Flow so UI automatically updates when data changes
     */
    fun getExpenses(): Flow<List<Expense>>
    
    /**
     * Get single expense by ID
     */
    suspend fun getExpenseById(id: Int): Expense?
    
    /**
     * Add new expense
     */
    suspend fun addExpense(expense: Expense)
    
    /**
     * Update existing expense
     */
    suspend fun updateExpense(expense: Expense)
    
    /**
     * Delete expense
     */
    suspend fun deleteExpense(id: Int)
}