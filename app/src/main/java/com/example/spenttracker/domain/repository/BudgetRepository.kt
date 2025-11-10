package com.example.spenttracker.domain.repository

import com.example.spenttracker.domain.model.Budget
import com.example.spenttracker.domain.model.BudgetAlert
import com.example.spenttracker.domain.model.BudgetSummary
import com.example.spenttracker.domain.model.BudgetWithSpending
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Budget Repository Interface
 * Defines operations for budget management
 */
interface BudgetRepository {

    // ========== CRUD Operations ==========

    /**
     * Create a new budget
     */
    suspend fun createBudget(budget: Budget): Result<Budget>

    /**
     * Update an existing budget
     */
    suspend fun updateBudget(budget: Budget): Result<Budget>

    /**
     * Delete a budget by ID
     */
    suspend fun deleteBudget(budgetId: Int): Result<Unit>

    /**
     * Get budget by ID
     */
    suspend fun getBudgetById(budgetId: Int): Result<Budget?>

    // ========== Query Operations ==========

    /**
     * Get all budgets for current user
     */
    fun getAllBudgets(): Flow<List<Budget>>

    /**
     * Get budgets for a specific month
     */
    fun getBudgetsForMonth(month: YearMonth): Flow<List<Budget>>

    /**
     * Get budget for specific category in a month
     */
    fun getBudgetForCategory(categoryId: Int, month: YearMonth): Flow<Budget?>

    // ========== Budget with Spending ==========

    /**
     * Get budget summary for a month
     * Includes total budget, total spent, category breakdowns, and alerts
     */
    fun getBudgetSummary(month: YearMonth): Flow<BudgetSummary>

    /**
     * Get budget with spending info for a specific budget
     */
    fun getBudgetWithSpending(budgetId: Int, month: YearMonth): Flow<BudgetWithSpending?>

    /**
     * Get all budgets with spending info for a month
     */
    fun getBudgetsWithSpending(month: YearMonth): Flow<List<BudgetWithSpending>>

    // ========== Alerts ==========

    /**
     * Create a budget alert
     */
    suspend fun createAlert(alert: BudgetAlert): Result<BudgetAlert>

    /**
     * Dismiss an alert
     */
    suspend fun dismissAlert(alertId: Int): Result<Unit>

    /**
     * Get all active (non-dismissed) alerts
     */
    fun getActiveAlerts(): Flow<List<BudgetAlert>>

    /**
     * Get alerts for a specific budget
     */
    fun getAlertsForBudget(budgetId: Int): Flow<List<BudgetAlert>>

    /**
     * Check if budget alerts should be triggered after expense change
     */
    suspend fun checkAndTriggerAlerts(categoryId: Int, month: YearMonth): Result<List<BudgetAlert>>

    // ========== Sync ==========

    /**
     * Sync budgets with backend
     */
    suspend fun syncBudgets(): Result<Unit>
}
