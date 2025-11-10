package com.example.spenttracker.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.spenttracker.data.local.entity.ExpenseEntity
import com.example.spenttracker.data.local.entity.CategoryEntity
import com.example.spenttracker.data.local.entity.BudgetEntity
import com.example.spenttracker.data.local.entity.BudgetAlertEntity
import com.example.spenttracker.data.local.entity.IncomeEntity

/**
 * Room database configuration
 * Updated to include Category support and sync tracking
 *
 * Version History (Like Laravel's migrations):
 * v1: Basic expenses
 * v2: Added categories
 * v3: Added category support
 * v4: Added sync tracking fields (server_id, sync_status, needs_sync, last_sync_at)
 * v5: Unified category ID system - CategoryEntity.id changed to Long, removed serverId field,
 *     ExpenseEntity.categoryId changed to Long to match
 * v6: Added @ColumnInfo annotation to userId field in ExpenseEntity
 * v7: Added Budget and BudgetAlert tables with type converters
 * v8: Added budget_type field to budgets table, made category_id nullable for overall budgets
 * v9: Added Income table for tracking income sources
 */
@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        BudgetAlertEntity::class,
        IncomeEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetAlertDao(): BudgetAlertDao
    abstract fun incomeDao(): IncomeDao
    
    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null
        
        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .fallbackToDestructiveMigration() // For development - recreate DB on version change
                .fallbackToDestructiveMigrationOnDowngrade() // Handle version downgrades
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8) // Explicitly handle migrations from these versions
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        // Function to clear the database instance (for testing/development)
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}