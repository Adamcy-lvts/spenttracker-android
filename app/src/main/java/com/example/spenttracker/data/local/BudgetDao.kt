package com.example.spenttracker.data.local

import androidx.room.*
import com.example.spenttracker.data.local.entity.BudgetAlertEntity
import com.example.spenttracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Budget DAO - Data Access Object for budget operations
 */
@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAllBudgets(userId: Int): Flow<List<BudgetEntity>>

    @Query("""
        SELECT * FROM budgets
        WHERE user_id = :userId
        AND budget_type = 'OVERALL'
        AND (
            (is_recurring = 1 AND start_date <= :monthEnd)
            OR (start_date <= :monthEnd AND (end_date IS NULL OR end_date >= :monthStart))
        )
        ORDER BY created_at DESC
        LIMIT 1
    """)
    fun getOverallBudget(
        userId: Int,
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): Flow<BudgetEntity?>

    @Query("""
        SELECT * FROM budgets
        WHERE user_id = :userId
        AND budget_type = 'CATEGORY'
        AND category_id = :categoryId
        AND (
            (is_recurring = 1 AND start_date <= :monthEnd)
            OR (start_date <= :monthEnd AND (end_date IS NULL OR end_date >= :monthStart))
        )
        ORDER BY created_at DESC
        LIMIT 1
    """)
    fun getBudgetForCategory(
        userId: Int,
        categoryId: Int,
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): Flow<BudgetEntity?>

    @Query("""
        SELECT * FROM budgets
        WHERE user_id = :userId
        AND (
            (is_recurring = 1 AND start_date <= :monthEnd)
            OR (start_date <= :monthEnd AND (end_date IS NULL OR end_date >= :monthStart))
        )
        ORDER BY created_at DESC
    """)
    fun getBudgetsForMonth(
        userId: Int,
        monthStart: LocalDate,
        monthEnd: LocalDate
    ): Flow<List<BudgetEntity>>

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteById(budgetId: Int)

    @Query("DELETE FROM budgets WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: Int)

    @Query("SELECT COUNT(*) FROM budgets WHERE user_id = :userId")
    suspend fun getBudgetCount(userId: Int): Int
}

/**
 * Budget Alert DAO - Data Access Object for budget alert operations
 */
@Dao
interface BudgetAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: BudgetAlertEntity): Long

    @Update
    suspend fun update(alert: BudgetAlertEntity)

    @Query("SELECT * FROM budget_alerts WHERE is_dismissed = 0 ORDER BY triggered_at DESC")
    fun getActiveAlerts(): Flow<List<BudgetAlertEntity>>

    @Query("UPDATE budget_alerts SET is_dismissed = 1 WHERE id = :alertId")
    suspend fun dismissAlert(alertId: Int)

    @Query("SELECT * FROM budget_alerts WHERE budget_id = :budgetId ORDER BY triggered_at DESC")
    fun getAlertsForBudget(budgetId: Int): Flow<List<BudgetAlertEntity>>

    @Query("DELETE FROM budget_alerts WHERE budget_id = :budgetId")
    suspend fun deleteAlertsForBudget(budgetId: Int)

    @Query("""
        SELECT * FROM budget_alerts
        WHERE budget_id = :budgetId
        AND alert_type = :alertType
        AND is_dismissed = 0
        ORDER BY triggered_at DESC
        LIMIT 1
    """)
    suspend fun getLatestAlertForType(
        budgetId: Int,
        alertType: String
    ): BudgetAlertEntity?
}
