package com.example.spenttracker.data.repository

import com.example.spenttracker.data.local.ExpenseDao
import com.example.spenttracker.data.mapper.toDomain
import com.example.spenttracker.data.mapper.toDomainList
import com.example.spenttracker.data.mapper.toEntity
import com.example.spenttracker.data.mapper.ExpenseDtoMapper
import com.example.spenttracker.data.remote.api.ExpenseApiService
import com.example.spenttracker.data.auth.UserContextProvider
import com.example.spenttracker.domain.model.Expense
import com.example.spenttracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation - handles both local database and API operations
 * Like Laravel's Service class that uses both Eloquent and HTTP Client
 */
@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val expenseApiService: ExpenseApiService,
    private val userContextProvider: UserContextProvider,
    private val expenseDtoMapper: ExpenseDtoMapper
) : ExpenseRepository {
    
    override fun getExpenses(): Flow<List<Expense>> {
        val userId = userContextProvider.getCurrentUserId()
        android.util.Log.d("ExpenseRepository", "getExpenses() called with userId: $userId")
        return if (userId != null) {
            android.util.Log.d("ExpenseRepository", "Using filtered query for userId: $userId")
            expenseDao.getAllExpenses(userId)
                .map { entities -> 
                    android.util.Log.d("ExpenseRepository", "Retrieved ${entities.size} expenses for user $userId")
                    entities.toDomainList() 
                }
        } else {
            android.util.Log.w("ExpenseRepository", "No user logged in - returning empty list")
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    
    override suspend fun getExpenseById(id: Int): Expense? {
        val userId = userContextProvider.getCurrentUserId()
        return if (userId != null) {
            expenseDao.getExpenseById(id, userId)?.toDomain()
        } else {
            null
        }
    }
    
    override suspend fun addExpense(expense: Expense) {
        // Save locally first - like Laravel's local transaction
        val userId = userContextProvider.getCurrentUserId()
        android.util.Log.d("ExpenseRepository", "addExpense() called with userId: $userId, original expense userId: ${expense.userId}")
        
        if (userId != null) {
            // Ensure expense is assigned to current user
            val expenseWithUserId = expense.copy(userId = userId.toInt())
            val entity = expenseWithUserId.toEntity()
            android.util.Log.d("ExpenseRepository", "Saving expense with userId: ${entity.userId} for user: $userId")
            expenseDao.insertExpense(entity)
        } else {
            android.util.Log.e("ExpenseRepository", "Cannot add expense - no user logged in!")
        }
    }
    
    override suspend fun updateExpense(expense: Expense) {
        // Update locally - like Laravel's update
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            // First, get the existing expense to preserve sync information
            val existingExpense = expenseDao.getExpenseById(expense.id, userId)
            if (existingExpense != null) {
                // Preserve server sync information but mark as needing sync
                val expenseWithUserId = expense.copy(userId = userId.toInt())
                val entity = expenseWithUserId.toEntity().copy(
                    serverId = existingExpense.serverId,  // Keep existing server ID
                    syncStatus = if (existingExpense.serverId != null) "PENDING" else "PENDING",
                    needsSync = true,  // Mark as needing sync since it was modified
                    lastSyncAt = existingExpense.lastSyncAt // Keep last sync time
                )
                expenseDao.updateExpense(entity)
                android.util.Log.d("ExpenseRepository", "Updated expense ${expense.id} and marked for sync")
            } else {
                android.util.Log.w("ExpenseRepository", "Cannot update expense ${expense.id} - not found for user $userId")
            }
        }
    }
    
    override suspend fun deleteExpense(id: Int) {
        // Soft delete for sync - like Laravel's soft delete
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            // Check if expense exists and belongs to current user
            val expense = expenseDao.getExpenseById(id, userId)
            if (expense != null) {
                if (expense.serverId != null) {
                    // Expense exists on server - soft delete to sync deletion
                    expenseDao.softDeleteExpense(id)
                    android.util.Log.d("ExpenseRepository", "Soft deleted expense $id for sync")
                } else {
                    // Local-only expense - can be physically deleted
                    expenseDao.deleteExpenseById(id)
                    android.util.Log.d("ExpenseRepository", "Physically deleted local-only expense $id")
                }
            } else {
                android.util.Log.w("ExpenseRepository", "Cannot delete expense $id - not found for user $userId")
            }
        } else {
            android.util.Log.e("ExpenseRepository", "Cannot delete expense - no user logged in!")
        }
    }
    
    /**
     * Migrate orphaned expenses (userId = 0) to current user
     * Called during login to fix data isolation
     */
    suspend fun migrateOrphanedExpensesToCurrentUser(): Int {
        val userId = userContextProvider.getCurrentUserId()
        return if (userId != null) {
            val migratedCount = expenseDao.migrateOrphanedExpenses(userId)
            android.util.Log.d("ExpenseRepository", "Migrated $migratedCount orphaned expenses to user $userId")
            migratedCount
        } else {
            android.util.Log.w("ExpenseRepository", "Cannot migrate orphaned expenses - no user logged in")
            0
        }
    }
    
    // API sync methods - like Laravel's HTTP Client calls
    
    /**
     * Sync expenses from API to local database
     * Like Laravel: $expenses = Http::get('/api/expenses'); foreach...
     */
    suspend fun syncExpensesFromApi(): Result<Unit> {
        return try {
            val response = expenseApiService.getExpenses()
            if (response.isSuccessful && response.body()?.success == true) {
                val apiExpenses = response.body()?.data ?: emptyList()
                val domainExpenses = expenseDtoMapper.toDomainModelList(apiExpenses)
                
                // Clear local data and insert API data (full sync)
                expenseDao.deleteAllExpenses()
                domainExpenses.forEach { expense ->
                    expenseDao.insertExpense(expense.toEntity())
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("API call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload local expense to API
     * Like Laravel: Http::post('/api/expenses', $data)
     */
    suspend fun uploadExpenseToApi(expense: Expense): Result<Expense> {
        return try {
            val createRequest = expenseDtoMapper.toCreateRequest(expense)
            val response = expenseApiService.createExpense(createRequest)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val apiExpense = response.body()?.data
                if (apiExpense != null) {
                    val updatedExpense = expenseDtoMapper.toDomainModel(apiExpense)
                    // Update local database with API response (includes server ID)
                    expenseDao.updateExpense(updatedExpense.toEntity())
                    Result.success(updatedExpense)
                } else {
                    Result.failure(Exception("No data in API response"))
                }
            } else {
                Result.failure(Exception("API call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update expense on API
     * Like Laravel: Http::put("/api/expenses/{$id}", $data)
     */
    suspend fun updateExpenseOnApi(expense: Expense): Result<Expense> {
        return try {
            val updateRequest = expenseDtoMapper.toUpdateRequest(expense)
            // Convert Int to Long for API call - like Laravel's (string) cast
            val response = expenseApiService.updateExpense(expense.id.toLong(), updateRequest)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val apiExpense = response.body()?.data
                if (apiExpense != null) {
                    val updatedExpense = expenseDtoMapper.toDomainModel(apiExpense)
                    Result.success(updatedExpense)
                } else {
                    Result.failure(Exception("No data in API response"))
                }
            } else {
                Result.failure(Exception("API call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete expense from API
     * Like Laravel: Http::delete("/api/expenses/{$id}")
     */
    suspend fun deleteExpenseFromApi(id: Int): Result<Unit> {
        return try {
            // Convert Int to Long for API call - like Laravel's (string) cast
            val response = expenseApiService.deleteExpense(id.toLong())
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API call failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
}