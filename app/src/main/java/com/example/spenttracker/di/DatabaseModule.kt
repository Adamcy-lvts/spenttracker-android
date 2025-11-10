package com.example.spenttracker.di

import android.content.Context
import androidx.room.Room
import com.example.spenttracker.data.local.ExpenseDao
import com.example.spenttracker.data.local.ExpenseDatabase
import com.example.spenttracker.data.local.CategoryDao
import com.example.spenttracker.data.local.BudgetDao
import com.example.spenttracker.data.local.BudgetAlertDao
import com.example.spenttracker.data.local.IncomeDao
import com.example.spenttracker.data.repository.ExpenseRepositoryImpl
import com.example.spenttracker.data.repository.CategoryRepositoryImpl
import com.example.spenttracker.data.repository.BudgetRepositoryImpl
import com.example.spenttracker.data.repository.IncomeRepositoryImpl
import com.example.spenttracker.domain.repository.ExpenseRepository
import com.example.spenttracker.domain.repository.CategoryRepository
import com.example.spenttracker.domain.repository.BudgetRepository
import com.example.spenttracker.domain.repository.IncomeRepository
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    
    // Like Laravel's Service Container binding
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository
    
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(
        incomeRepositoryImpl: IncomeRepositoryImpl
    ): IncomeRepository

    companion object {
        
        @Provides
        @Singleton
        fun provideExpenseDatabase(
            @ApplicationContext context: Context
        ): ExpenseDatabase {
            return Room.databaseBuilder(
                context,
                ExpenseDatabase::class.java,
                "expense_database"
            )
            .fallbackToDestructiveMigration() // For development - recreate DB on version change
            .fallbackToDestructiveMigrationOnDowngrade() // Handle version downgrades
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8) // Explicitly handle migrations from these versions
            .build()
        }

        @Provides
        fun provideExpenseDao(database: ExpenseDatabase): ExpenseDao {
            return database.expenseDao()
        }

        @Provides
        fun provideCategoryDao(database: ExpenseDatabase): CategoryDao {
            return database.categoryDao()
        }

        @Provides
        fun provideBudgetDao(database: ExpenseDatabase): BudgetDao {
            return database.budgetDao()
        }

        @Provides
        fun provideBudgetAlertDao(database: ExpenseDatabase): BudgetAlertDao {
            return database.budgetAlertDao()
        }

        @Provides
        fun provideIncomeDao(database: ExpenseDatabase): IncomeDao {
            return database.incomeDao()
        }
    }
}