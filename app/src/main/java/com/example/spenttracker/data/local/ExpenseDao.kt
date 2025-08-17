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
     * Get all expenses, ordered by date (newest first)
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    
    /**
     * Get expenses with category information
     */
    @Query("""
        SELECT e.*, c.name as category_name, c.color as category_color
        FROM expenses e
        LEFT JOIN categories c ON e.category_id = c.id
        ORDER BY e.date DESC, e.id DESC
    """)
    fun getExpensesWithCategories(): Flow<List<ExpenseWithCategory>>
    
    /**
     * Get single expense by ID
     */
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Int): ExpenseEntity?
    
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
     * Delete all expenses
     */
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
    
    // Data class for expense with category information
    data class ExpenseWithCategory(
        val id: Int,
        val description: String,
        val amount: Double,
        val date: String,
        @ColumnInfo(name = "category_id") val categoryId: Int?,
        val userId: Int,
        val createdAt: String,
        val updatedAt: String,
        @ColumnInfo(name = "category_name") val categoryName: String?,
        @ColumnInfo(name = "category_color") val categoryColor: String?
    )
}