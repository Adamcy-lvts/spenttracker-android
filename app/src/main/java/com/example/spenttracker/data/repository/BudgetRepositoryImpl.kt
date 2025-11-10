package com.example.spenttracker.data.repository

import android.util.Log
import com.example.spenttracker.data.auth.UserContextProviderImpl
import com.example.spenttracker.data.local.BudgetAlertDao
import com.example.spenttracker.data.local.BudgetDao
import com.example.spenttracker.data.local.CategoryDao
import com.example.spenttracker.data.local.ExpenseDao
import com.example.spenttracker.data.local.entity.BudgetAlertType
import com.example.spenttracker.data.mapper.*
import com.example.spenttracker.domain.model.*
import com.example.spenttracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Budget Repository Implementation
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val budgetAlertDao: BudgetAlertDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val userContextProvider: UserContextProviderImpl
) : BudgetRepository {

    companion object {
        private const val TAG = "BudgetRepository"
    }

    // ========== CRUD Operations ==========

    override suspend fun createBudget(budget: Budget): Result<Budget> {
        return try {
            val userId = userContextProvider.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val entity = budget.copy(userId = userId.toInt()).toEntity()
            val id = budgetDao.insert(entity)
            val created = budget.copy(id = id.toInt())

            Log.d(TAG, "Budget created: $created")
            Result.success(created)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating budget", e)
            Result.failure(e)
        }
    }

    override suspend fun updateBudget(budget: Budget): Result<Budget> {
        return try {
            val entity = budget.toEntity()
            budgetDao.update(entity)

            Log.d(TAG, "Budget updated: $budget")
            Result.success(budget)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating budget", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(budgetId: Int): Result<Unit> {
        return try {
            budgetDao.deleteById(budgetId)
            budgetAlertDao.deleteAlertsForBudget(budgetId)

            Log.d(TAG, "Budget deleted: $budgetId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting budget", e)
            Result.failure(e)
        }
    }

    override suspend fun getBudgetById(budgetId: Int): Result<Budget?> {
        return try {
            val entity = budgetDao.getBudgetById(budgetId)
            val budget = entity?.let {
                val category = it.categoryId?.let { id -> categoryDao.getCategoryById(id.toLong()) }
                it.toDomain(category?.name ?: "", category?.color ?: "")
            }
            Result.success(budget)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting budget", e)
            Result.failure(e)
        }
    }

    // ========== Query Operations ==========

    override fun getAllBudgets(): Flow<List<Budget>> {
        val userId = userContextProvider.getCurrentUserId()?.toInt() ?: return flowOf(emptyList())

        return budgetDao.getAllBudgets(userId).map { entities ->
            // Get category info for category budgets only
            val categoryIds = entities.mapNotNull { it.categoryId?.toLong() }.distinct()
            val categories = categoryIds.mapNotNull { categoryDao.getCategoryById(it) }
            val categoryMap = categories.associate { it.id.toInt() to (it.name to it.color) }

            entities.toDomainList(categoryMap)
        }
    }

    override fun getBudgetsForMonth(month: YearMonth): Flow<List<Budget>> {
        val userId = userContextProvider.getCurrentUserId()?.toInt() ?: return flowOf(emptyList())

        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        return budgetDao.getBudgetsForMonth(userId, monthStart, monthEnd).map { entities ->
            val categoryIds = entities.mapNotNull { it.categoryId?.toLong() }.distinct()
            val categories = categoryIds.mapNotNull { categoryDao.getCategoryById(it) }
            val categoryMap = categories.associate { it.id.toInt() to (it.name to it.color) }

            entities.toDomainList(categoryMap)
        }
    }

    override fun getBudgetForCategory(categoryId: Int, month: YearMonth): Flow<Budget?> {
        val userId = userContextProvider.getCurrentUserId()?.toInt() ?: return flowOf(null)

        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        return budgetDao.getBudgetForCategory(userId, categoryId, monthStart, monthEnd).map { entity ->
            entity?.let {
                val category = it.categoryId?.let { id -> categoryDao.getCategoryById(id.toLong()) }
                it.toDomain(category?.name ?: "", category?.color ?: "")
            }
        }
    }

    /**
     * Get overall budget for a month
     */
    fun getOverallBudget(month: YearMonth): Flow<Budget?> {
        val userId = userContextProvider.getCurrentUserId()?.toInt() ?: return flowOf(null)

        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        return budgetDao.getOverallBudget(userId, monthStart, monthEnd).map { entity ->
            entity?.toDomain("Overall Budget", "")
        }
    }

    // ========== Budget with Spending ==========

    override fun getBudgetSummary(month: YearMonth): Flow<BudgetSummary> {
        val userId = userContextProvider.getCurrentUserId()?.toInt() ?: return flowOf(
            BudgetSummary(null, emptyList(), 0.0, emptyList())
        )

        return combine(
            getOverallBudget(month),
            getBudgetsWithSpending(month),
            getActiveAlerts()
        ) { overallBudget, categoryBudgetsWithSpending, alerts ->
            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()

            // Get overall budget with spending if it exists
            val overallBudgetWithSpending = overallBudget?.let { budget ->
                val allExpenses = expenseDao.getExpensesByDateRange(userId, monthStart, monthEnd)
                val totalSpent = allExpenses.sumOf { it.amount }

                val remaining = budget.amount - totalSpent
                val percentageUsed = if (budget.amount > 0) {
                    ((totalSpent / budget.amount) * 100).toInt()
                } else 0

                val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), monthEnd).toInt()

                val status = when {
                    percentageUsed >= 100 && totalSpent > budget.amount -> BudgetStatus.OVER_BUDGET
                    percentageUsed >= 100 -> BudgetStatus.CRITICAL
                    percentageUsed >= 80 -> BudgetStatus.WARNING
                    else -> BudgetStatus.SAFE
                }

                BudgetWithSpending(
                    budget = budget,
                    spent = totalSpent,
                    remaining = remaining,
                    percentageUsed = percentageUsed,
                    daysLeft = daysLeft.coerceAtLeast(0),
                    status = status
                )
            }

            // Calculate unbudgeted expenses (only for category budgets)
            val unbudgetedSpent = if (overallBudget == null) {
                val budgetedCategoryIds = categoryBudgetsWithSpending.mapNotNull { it.budget.categoryId }
                val allExpenses = expenseDao.getExpensesByDateRange(userId, monthStart, monthEnd)
                val unbudgetedExpenses = allExpenses.filter { expense ->
                    expense.categoryId?.toInt() !in budgetedCategoryIds
                }
                unbudgetedExpenses.sumOf { it.amount }
            } else {
                0.0  // No unbudgeted concept when using overall budget
            }

            BudgetSummary(
                overallBudget = overallBudgetWithSpending,
                categoryBudgets = categoryBudgetsWithSpending,
                unbudgetedSpent = unbudgetedSpent,
                alerts = alerts
            )
        }
    }

    override fun getBudgetWithSpending(budgetId: Int, month: YearMonth): Flow<BudgetWithSpending?> {
        return flow {
            val budgetResult = getBudgetById(budgetId)
            val budget = budgetResult.getOrNull() ?: run {
                emit(null)
                return@flow
            }

            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()
            val userId = userContextProvider.getCurrentUserId()?.toInt() ?: return@flow

            val allExpenses = expenseDao.getExpensesByDateRange(userId, monthStart, monthEnd)

            // Calculate spending based on budget type
            val spent = when (budget.budgetType) {
                com.example.spenttracker.data.local.entity.BudgetType.OVERALL -> {
                    // Overall budget: sum all expenses
                    allExpenses.sumOf { it.amount }
                }
                com.example.spenttracker.data.local.entity.BudgetType.CATEGORY -> {
                    // Category budget: sum only expenses for this category
                    allExpenses.filter { it.categoryId?.toInt() == budget.categoryId }
                        .sumOf { it.amount }
                }
            }

            val remaining = budget.amount - spent
            val percentageUsed = if (budget.amount > 0) {
                ((spent / budget.amount) * 100).toInt()
            } else 0

            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), monthEnd).toInt()

            val status = when {
                percentageUsed >= 100 && spent > budget.amount -> BudgetStatus.OVER_BUDGET
                percentageUsed >= 100 -> BudgetStatus.CRITICAL
                percentageUsed >= 80 -> BudgetStatus.WARNING
                else -> BudgetStatus.SAFE
            }

            emit(
                BudgetWithSpending(
                    budget = budget,
                    spent = spent,
                    remaining = remaining,
                    percentageUsed = percentageUsed,
                    daysLeft = daysLeft.coerceAtLeast(0),
                    status = status
                )
            )
        }
    }

    override fun getBudgetsWithSpending(month: YearMonth): Flow<List<BudgetWithSpending>> {
        return getBudgetsForMonth(month).map { budgets ->
            budgets.mapNotNull { budget ->
                getBudgetWithSpending(budget.id, month).first()
            }
        }
    }

    // ========== Alerts ==========

    override suspend fun createAlert(alert: BudgetAlert): Result<BudgetAlert> {
        return try {
            val entity = alert.toEntity()
            val id = budgetAlertDao.insert(entity)
            val created = alert.copy(id = id.toInt())

            Log.d(TAG, "Budget alert created: $created")
            Result.success(created)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating alert", e)
            Result.failure(e)
        }
    }

    override suspend fun dismissAlert(alertId: Int): Result<Unit> {
        return try {
            budgetAlertDao.dismissAlert(alertId)
            Log.d(TAG, "Alert dismissed: $alertId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing alert", e)
            Result.failure(e)
        }
    }

    override fun getActiveAlerts(): Flow<List<BudgetAlert>> {
        return budgetAlertDao.getActiveAlerts().map { entities ->
            // Get budget and category info for alerts
            entities.mapNotNull { entity ->
                val budget = budgetDao.getBudgetById(entity.budgetId)
                val category = budget?.categoryId?.let { categoryDao.getCategoryById(it.toLong()) }
                entity.toDomain(category?.name ?: "Overall Budget", category?.color ?: "")
            }
        }
    }

    override fun getAlertsForBudget(budgetId: Int): Flow<List<BudgetAlert>> {
        return budgetAlertDao.getAlertsForBudget(budgetId).map { entities ->
            val budget = budgetDao.getBudgetById(budgetId)
            val category = budget?.categoryId?.let { categoryDao.getCategoryById(it.toLong()) }
            val categoryName = category?.name ?: "Overall Budget"
            val categoryColor = category?.color ?: ""

            entities.map { it.toDomain(categoryName, categoryColor) }
        }
    }

    override suspend fun checkAndTriggerAlerts(categoryId: Int, month: YearMonth): Result<List<BudgetAlert>> {
        return try {
            val userId = userContextProvider.getCurrentUserId()?.toInt()
                ?: return Result.success(emptyList())

            val budget = getBudgetForCategory(categoryId, month).first()
                ?: return Result.success(emptyList())

            if (!budget.enableNotifications) {
                return Result.success(emptyList())
            }

            val budgetWithSpending = getBudgetWithSpending(budget.id, month).first()
                ?: return Result.success(emptyList())

            val triggeredAlerts = mutableListOf<BudgetAlert>()

            // Check 80% threshold
            if (budget.alertAt80 && budgetWithSpending.percentageUsed >= 80 && budgetWithSpending.percentageUsed < 100) {
                val existingAlert = budgetAlertDao.getLatestAlertForType(
                    budget.id,
                    BudgetAlertType.THRESHOLD_80.name
                )
                if (existingAlert == null) {
                    val alert = BudgetAlert(
                        budgetId = budget.id,
                        alertType = BudgetAlertType.THRESHOLD_80,
                        spentAmount = budgetWithSpending.spent,
                        budgetAmount = budget.amount,
                        categoryName = budget.categoryName,
                        categoryColor = budget.categoryColor
                    )
                    createAlert(alert).getOrNull()?.let { triggeredAlerts.add(it) }
                }
            }

            // Check 100% threshold
            if (budget.alertAt100 && budgetWithSpending.percentageUsed >= 100 && !budgetWithSpending.isOverBudget) {
                val existingAlert = budgetAlertDao.getLatestAlertForType(
                    budget.id,
                    BudgetAlertType.THRESHOLD_100.name
                )
                if (existingAlert == null) {
                    val alert = BudgetAlert(
                        budgetId = budget.id,
                        alertType = BudgetAlertType.THRESHOLD_100,
                        spentAmount = budgetWithSpending.spent,
                        budgetAmount = budget.amount,
                        categoryName = budget.categoryName,
                        categoryColor = budget.categoryColor
                    )
                    createAlert(alert).getOrNull()?.let { triggeredAlerts.add(it) }
                }
            }

            // Check over budget
            if (budget.alertOverBudget && budgetWithSpending.isOverBudget) {
                val existingAlert = budgetAlertDao.getLatestAlertForType(
                    budget.id,
                    BudgetAlertType.OVER_BUDGET.name
                )
                if (existingAlert == null) {
                    val alert = BudgetAlert(
                        budgetId = budget.id,
                        alertType = BudgetAlertType.OVER_BUDGET,
                        spentAmount = budgetWithSpending.spent,
                        budgetAmount = budget.amount,
                        categoryName = budget.categoryName,
                        categoryColor = budget.categoryColor
                    )
                    createAlert(alert).getOrNull()?.let { triggeredAlerts.add(it) }
                }
            }

            Log.d(TAG, "Triggered ${triggeredAlerts.size} alerts for budget ${budget.id}")
            Result.success(triggeredAlerts)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking alerts", e)
            Result.failure(e)
        }
    }

    // ========== Sync ==========

    override suspend fun syncBudgets(): Result<Unit> {
        return try {
            // TODO: Implement backend sync when API is ready
            Log.d(TAG, "Budget sync not yet implemented")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing budgets", e)
            Result.failure(e)
        }
    }
}
