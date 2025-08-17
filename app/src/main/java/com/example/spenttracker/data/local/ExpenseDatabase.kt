package com.example.spenttracker.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.spenttracker.data.local.entity.ExpenseEntity
import com.example.spenttracker.data.local.entity.CategoryEntity

/**
 * Room database configuration
 * Updated to include Category support
 */
@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    
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
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}