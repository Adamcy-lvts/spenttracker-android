package com.example.spenttracker.data.repository

import com.example.spenttracker.data.local.ExpenseDao
import com.example.spenttracker.data.mapper.toDomain
import com.example.spenttracker.data.mapper.toDomainList
import com.example.spenttracker.data.mapper.toEntity
import com.example.spenttracker.domain.model.Expense
import com.example.spenttracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository implementation - connects to local database
 * This handles all the data operations using Room database
 */
class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    
    override fun getExpenses(): Flow<List<Expense>> {
        return expenseDao.getAllExpenses()
            .map { entities -> entities.toDomainList() }
    }
    
    override suspend fun getExpenseById(id: Int): Expense? {
        return expenseDao.getExpenseById(id)?.toDomain()
    }
    
    override suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }
    
    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toEntity())
    }
    
    override suspend fun deleteExpense(id: Int) {
        expenseDao.deleteExpenseById(id)
    }
}