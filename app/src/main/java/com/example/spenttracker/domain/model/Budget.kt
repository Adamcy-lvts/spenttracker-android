package com.example.spenttracker.domain.model

import com.example.spenttracker.data.local.entity.BudgetAlertType
import com.example.spenttracker.data.local.entity.BudgetPeriodType
import com.example.spenttracker.data.local.entity.BudgetType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Budget Domain Model
 */
data class Budget(
    val id: Int = 0,
    val budgetType: BudgetType = BudgetType.CATEGORY,
    val categoryId: Int? = null,  // Null for overall budgets
    val categoryName: String = "",
    val categoryColor: String = "",
    val userId: Int,
    val amount: Double,
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isRecurring: Boolean = true,
    val alertAt80: Boolean = true,
    val alertAt100: Boolean = true,
    val alertOverBudget: Boolean = true,
    val enableNotifications: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val remoteId: Int? = null,
    val isSynced: Boolean = false
)

/**
 * Budget with spending information
 */
data class BudgetWithSpending(
    val budget: Budget,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Int,
    val daysLeft: Int,
    val status: BudgetStatus
) {
    val isOverBudget: Boolean
        get() = spent > budget.amount

    val formattedPercentage: String
        get() = "$percentageUsed%"
}

/**
 * Budget Status based on percentage used
 */
enum class BudgetStatus {
    SAFE,        // < 80%
    WARNING,     // 80-99%
    CRITICAL,    // 100%
    OVER_BUDGET  // > 100%
}

/**
 * Budget Alert
 */
data class BudgetAlert(
    val id: Int = 0,
    val budgetId: Int,
    val alertType: BudgetAlertType,
    val triggeredAt: LocalDateTime = LocalDateTime.now(),
    val isDismissed: Boolean = false,
    val spentAmount: Double,
    val budgetAmount: Double,
    val categoryName: String = "",
    val categoryColor: String = ""
) {
    val percentageUsed: Int
        get() = if (budgetAmount > 0) {
            ((spentAmount / budgetAmount) * 100).toInt()
        } else 0

    val message: String
        get() = when (alertType) {
            BudgetAlertType.THRESHOLD_80 -> "You've used 80% of your $categoryName budget"
            BudgetAlertType.THRESHOLD_100 -> "You've reached your $categoryName budget limit"
            BudgetAlertType.OVER_BUDGET -> "You're over budget for $categoryName"
        }
}

/**
 * Budget Summary for overview screen
 */
data class BudgetSummary(
    val overallBudget: BudgetWithSpending? = null,  // Overall budget if exists
    val categoryBudgets: List<BudgetWithSpending>,
    val unbudgetedSpent: Double,
    val alerts: List<BudgetAlert>
) {
    // Calculate totals based on budget configuration
    val totalBudget: Double
        get() = overallBudget?.budget?.amount ?: categoryBudgets.sumOf { it.budget.amount }

    val totalSpent: Double
        get() = overallBudget?.spent ?: categoryBudgets.sumOf { it.spent }

    val totalRemaining: Double
        get() = totalBudget - totalSpent

    val percentageUsed: Int
        get() = if (totalBudget > 0) {
            ((totalSpent / totalBudget) * 100).toInt()
        } else 0

    val hasBudgets: Boolean
        get() = overallBudget != null || categoryBudgets.isNotEmpty()

    val hasAlerts: Boolean
        get() = alerts.any { !it.isDismissed }

    val status: BudgetStatus
        get() = when {
            percentageUsed >= 100 && totalSpent > totalBudget -> BudgetStatus.OVER_BUDGET
            percentageUsed >= 100 -> BudgetStatus.CRITICAL
            percentageUsed >= 80 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
}
