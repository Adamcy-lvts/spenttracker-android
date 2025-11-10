package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.entity.BudgetAlertEntity
import com.example.spenttracker.data.local.entity.BudgetEntity
import com.example.spenttracker.domain.model.Budget
import com.example.spenttracker.domain.model.BudgetAlert

/**
 * Convert BudgetEntity to Budget domain model
 */
fun BudgetEntity.toDomain(categoryName: String = "", categoryColor: String = ""): Budget {
    return Budget(
        id = id,
        budgetType = budgetType,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColor = categoryColor,
        userId = userId,
        amount = amount,
        periodType = periodType,
        startDate = startDate,
        endDate = endDate,
        isRecurring = isRecurring,
        alertAt80 = alertAt80,
        alertAt100 = alertAt100,
        alertOverBudget = alertOverBudget,
        enableNotifications = enableNotifications,
        createdAt = createdAt,
        updatedAt = updatedAt,
        remoteId = remoteId,
        isSynced = isSynced
    )
}

/**
 * Convert Budget domain model to BudgetEntity
 */
fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        budgetType = budgetType,
        categoryId = categoryId,
        userId = userId,
        amount = amount,
        periodType = periodType,
        startDate = startDate,
        endDate = endDate,
        isRecurring = isRecurring,
        alertAt80 = alertAt80,
        alertAt100 = alertAt100,
        alertOverBudget = alertOverBudget,
        enableNotifications = enableNotifications,
        createdAt = createdAt,
        updatedAt = updatedAt,
        remoteId = remoteId,
        isSynced = isSynced
    )
}

/**
 * Convert BudgetAlertEntity to BudgetAlert domain model
 */
fun BudgetAlertEntity.toDomain(categoryName: String = "", categoryColor: String = ""): BudgetAlert {
    return BudgetAlert(
        id = id,
        budgetId = budgetId,
        alertType = alertType,
        triggeredAt = triggeredAt,
        isDismissed = isDismissed,
        spentAmount = spentAmount,
        budgetAmount = budgetAmount,
        categoryName = categoryName,
        categoryColor = categoryColor
    )
}

/**
 * Convert BudgetAlert domain model to BudgetAlertEntity
 */
fun BudgetAlert.toEntity(): BudgetAlertEntity {
    return BudgetAlertEntity(
        id = id,
        budgetId = budgetId,
        alertType = alertType,
        triggeredAt = triggeredAt,
        isDismissed = isDismissed,
        spentAmount = spentAmount,
        budgetAmount = budgetAmount
    )
}

/**
 * Convert list of BudgetEntity to list of Budget
 */
fun List<BudgetEntity>.toDomainList(categoryMap: Map<Int, Pair<String, String>> = emptyMap()): List<Budget> {
    return map { entity ->
        val (name, color) = if (entity.categoryId != null) {
            categoryMap[entity.categoryId] ?: ("" to "")
        } else {
            "" to ""  // Overall budget has no category
        }
        entity.toDomain(name, color)
    }
}

/**
 * Convert list of BudgetAlertEntity to list of BudgetAlert
 */
fun List<BudgetAlertEntity>.toAlertDomainList(categoryMap: Map<Int, Pair<String, String>> = emptyMap()): List<BudgetAlert> {
    return map { entity ->
        val (name, color) = categoryMap[entity.budgetId] ?: ("" to "")
        entity.toDomain(name, color)
    }
}
