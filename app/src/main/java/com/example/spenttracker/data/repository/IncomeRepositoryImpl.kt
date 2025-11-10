package com.example.spenttracker.data.repository

import com.example.spenttracker.data.local.IncomeDao
import com.example.spenttracker.data.mapper.toDomain
import com.example.spenttracker.data.mapper.toDomainList
import com.example.spenttracker.data.mapper.toEntity
import com.example.spenttracker.data.auth.UserContextProvider
import com.example.spenttracker.domain.model.Income
import com.example.spenttracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for Income operations
 * Handles both local database and API operations (when backend is ready)
 */
@Singleton
class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao,
    private val userContextProvider: UserContextProvider
    // TODO: Add IncomeApiService when backend API is ready
) : IncomeRepository {

    override fun getAllIncomes(): Flow<List<Income>> {
        val userId = userContextProvider.getCurrentUserId()
        android.util.Log.d("IncomeRepository", "getAllIncomes() called with userId: $userId")
        return if (userId != null) {
            incomeDao.getAllIncomes(userId)
                .map { entities ->
                    android.util.Log.d("IncomeRepository", "Retrieved ${entities.size} incomes for user $userId")
                    entities.toDomainList()
                }
        } else {
            android.util.Log.w("IncomeRepository", "No user logged in - returning empty list")
            flowOf(emptyList())
        }
    }

    override fun getIncomesForMonth(yearMonth: YearMonth): Flow<List<Income>> {
        val userId = userContextProvider.getCurrentUserId() ?: return flowOf(emptyList())

        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()

        return incomeDao.getIncomesForPeriod(userId, startDate, endDate)
            .map { it.toDomainList() }
    }

    override suspend fun getTotalIncomeForMonth(yearMonth: YearMonth): Double {
        val userId = userContextProvider.getCurrentUserId() ?: return 0.0

        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()

        return incomeDao.getTotalIncomeForPeriod(userId, startDate, endDate)
    }

    override fun getRecurringIncomes(): Flow<List<Income>> {
        val userId = userContextProvider.getCurrentUserId() ?: return flowOf(emptyList())
        return incomeDao.getRecurringIncomes(userId).map { it.toDomainList() }
    }

    override fun getIncomesBySource(source: String): Flow<List<Income>> {
        val userId = userContextProvider.getCurrentUserId() ?: return flowOf(emptyList())
        return incomeDao.getIncomesBySource(userId, source).map { it.toDomainList() }
    }

    override fun getIncomeSources(): Flow<List<String>> {
        val userId = userContextProvider.getCurrentUserId() ?: return flowOf(emptyList())
        return incomeDao.getIncomeSources(userId)
    }

    override suspend fun addIncome(income: Income): Result<Income> {
        return try {
            val userId = userContextProvider.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val incomeWithUserId = income.copy(userId = userId)
            val entity = incomeWithUserId.toEntity()

            val insertedId = incomeDao.insertIncome(entity)
            val insertedEntity = incomeDao.getIncomeById(insertedId.toInt())

            if (insertedEntity != null) {
                android.util.Log.d("IncomeRepository", "Income added successfully: ID=$insertedId")
                Result.success(insertedEntity.toDomain())
            } else {
                Result.failure(Exception("Failed to retrieve inserted income"))
            }
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "Error adding income", e)
            Result.failure(e)
        }
    }

    override suspend fun updateIncome(income: Income): Result<Income> {
        return try {
            val userId = userContextProvider.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val incomeWithUserId = income.copy(userId = userId)
            val entity = incomeWithUserId.toEntity()

            incomeDao.updateIncome(entity)

            val updatedEntity = incomeDao.getIncomeById(income.id)
            if (updatedEntity != null) {
                android.util.Log.d("IncomeRepository", "Income updated successfully: ID=${income.id}")
                Result.success(updatedEntity.toDomain())
            } else {
                Result.failure(Exception("Failed to retrieve updated income"))
            }
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "Error updating income", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteIncome(incomeId: Int): Result<Unit> {
        return try {
            incomeDao.deleteIncomeById(incomeId)
            android.util.Log.d("IncomeRepository", "Income deleted successfully: ID=$incomeId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("IncomeRepository", "Error deleting income", e)
            Result.failure(e)
        }
    }

    override suspend fun syncIncomes(): Result<Unit> {
        // TODO: Implement sync with backend API when ready
        return Result.success(Unit)
    }

    override suspend fun getIncomeCount(): Int {
        val userId = userContextProvider.getCurrentUserId() ?: return 0
        return incomeDao.getIncomeCount(userId)
    }
}
