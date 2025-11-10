package com.example.spenttracker.data.local

import androidx.room.*
import com.example.spenttracker.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for expenses
 * Defines how to access expense data in the database
 */
@Dao
interface ExpenseDao {
    
    /**
     * Get all expenses for a specific user, ordered by date (newest first)
     * Excludes soft-deleted expenses
     */
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND sync_status != 'DELETED' ORDER BY date DESC, id DESC")
    fun getAllExpenses(userId: Long): Flow<List<ExpenseEntity>>
    
    /**
     * Get all expenses (for backward compatibility - should be avoided)
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpensesUnsafe(): Flow<List<ExpenseEntity>>
    
    /**
     * Get expenses with category information for a specific user
     * Excludes soft-deleted expenses
     */
    @Query("""
        SELECT e.*, c.name as category_name, c.color as category_color
        FROM expenses e
        LEFT JOIN categories c ON e.category_id = c.id
        WHERE e.user_id = :userId AND e.sync_status != 'DELETED'
        ORDER BY e.date DESC, e.id DESC
    """)
    fun getExpensesWithCategories(userId: Long): Flow<List<ExpenseWithCategory>>
    
    /**
     * Get single expense by ID for a specific user
     * Excludes soft-deleted expenses
     */
    @Query("SELECT * FROM expenses WHERE id = :id AND user_id = :userId AND sync_status != 'DELETED'")
    suspend fun getExpenseById(id: Int, userId: Long): ExpenseEntity?
    
    /**
     * Get all expenses for a specific user as a list (for sync operations)
     * Excludes soft-deleted expenses
     */
    @Query("SELECT * FROM expenses WHERE user_id = :userId AND sync_status != 'DELETED' ORDER BY date DESC, id DESC")
    suspend fun getAllExpensesByUser(userId: Long): List<ExpenseEntity>
    
    /**
     * Update category ID references when a category ID changes (for sync operations)
     */
    @Query("UPDATE expenses SET category_id = :newCategoryId WHERE category_id = :oldCategoryId AND user_id = :userId")
    suspend fun updateExpensesCategoryId(oldCategoryId: Long, newCategoryId: Long, userId: Long)
    
    /**
     * Insert new expense
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long
    
    /**
     * Insert multiple expenses
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)
    
    /**
     * Update expense
     */
    @Update
    suspend fun updateExpense(expense: ExpenseEntity)
    
    /**
     * Delete expense
     */
    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
    
    /**
     * Delete expense by ID
     */
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)
    
    /**
     * Soft delete expense by marking as deleted (for sync)
     */
    @Query("UPDATE expenses SET sync_status = 'DELETED', needs_sync = 1 WHERE id = :id")
    suspend fun softDeleteExpense(id: Int)
    
    /**
     * Delete all expenses
     */
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
    
    /**
     * Migrate orphaned expenses (userId = 0) to a specific user
     */
    @Query("UPDATE expenses SET user_id = :userId WHERE user_id = 0")
    suspend fun migrateOrphanedExpenses(userId: Long): Int
    
    // Sync-related queries - Like Laravel's sync database operations
    
    /**
     * Get expenses that need to be synced to server for a specific user
     * Like Laravel: Expense::where('needs_sync', true)->where('user_id', $userId)->get()
     */
    @Query("SELECT * FROM expenses WHERE needs_sync = 1 AND user_id = :userId")
    suspend fun getExpensesNeedingSync(userId: Long): List<ExpenseEntity>
    
    /**
     * Get expenses by sync status
     * Like Laravel: Expense::where('sync_status', $status)->get()
     */
    @Query("SELECT * FROM expenses WHERE sync_status = :status")
    suspend fun getExpensesBySyncStatus(status: String): List<ExpenseEntity>
    
    /**
     * Update expense sync status
     * Like Laravel: $expense->update(['sync_status' => $status])
     */
    @Query("UPDATE expenses SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String)
    
    /**
     * Mark expense as synced with server ID
     * Like Laravel: $expense->update(['server_id' => $id, 'needs_sync' => false])
     */
    @Query("UPDATE expenses SET server_id = :serverId, sync_status = :status, needs_sync = 0, last_sync_at = :syncTime WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Long, status: String, syncTime: String)
    
    /**
     * Mark expense as needing sync (when edited locally)
     * Like Laravel: $expense->update(['needs_sync' => true])
     */
    @Query("UPDATE expenses SET needs_sync = 1, sync_status = :status WHERE id = :id")
    suspend fun markAsNeedingSync(id: Int, status: String = "PENDING")
    
    /**
     * Get expenses by date range for budget calculations
     */
    @Query("""
        SELECT * FROM expenses
        WHERE user_id = :userId
        AND date BETWEEN :startDate AND :endDate
        AND sync_status != 'DELETED'
        ORDER BY date DESC
    """)
    suspend fun getExpensesByDateRange(
        userId: Int,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): List<ExpenseEntity>

    /**
     * Get expense by server ID for a specific user
     * Like Laravel: Expense::where('server_id', $serverId)->where('user_id', $userId)->first()
     */
    @Query("SELECT * FROM expenses WHERE server_id = :serverId AND user_id = :userId")
    suspend fun getExpenseByServerId(serverId: Long, userId: Long): ExpenseEntity?
    
    /**
     * Update server info after successful sync
     * Like Laravel: $expense->update(['server_id' => $serverId, 'sync_status' => $status, 'needs_sync' => false])
     */
    @Query("UPDATE expenses SET server_id = :serverId, sync_status = :syncStatus, needs_sync = :needsSync, last_sync_at = :lastSyncAt WHERE id = :localId")
    suspend fun updateServerInfo(localId: Int, serverId: Long, syncStatus: String, lastSyncAt: String, needsSync: Boolean)
    
    /**
     * Get expense count by user (excluding deleted)
     */
    @Query("SELECT COUNT(*) FROM expenses WHERE user_id = :userId AND sync_status != 'DELETED'")
    suspend fun getExpenseCountByUser(userId: Long): Int
    
    /**
     * Get expense count by sync status for a user
     */
    @Query("SELECT COUNT(*) FROM expenses WHERE user_id = :userId AND sync_status = :status")
    suspend fun getExpenseCountByStatus(userId: Long, status: String): Int
    
    // Data class for expense with category information
    data class ExpenseWithCategory(
        val id: Int,
        val description: String,
        val amount: Double,
        val date: String,
        @ColumnInfo(name = "category_id") val categoryId: Long?,
        @ColumnInfo(name = "user_id") val userId: Int,
        val createdAt: String,
        val updatedAt: String,
        @ColumnInfo(name = "category_name") val categoryName: String?,
        @ColumnInfo(name = "category_color") val categoryColor: String?
    )
}