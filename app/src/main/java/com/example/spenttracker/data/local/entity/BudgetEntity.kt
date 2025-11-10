package com.example.spenttracker.data.local.entity

import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Budget Period Type
 */
enum class BudgetPeriodType {
    MONTHLY,
    CUSTOM
}

/**
 * Budget Type - Overall budget or category-specific
 */
enum class BudgetType {
    OVERALL,    // One budget for all expenses
    CATEGORY    // Budget per category
}

/**
 * Budget Entity - Room database table for budgets
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("category_id"),
        Index("user_id"),
        Index(value = ["user_id", "category_id"]),
        Index(value = ["user_id", "budget_type", "start_date"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "budget_type")
    val budgetType: BudgetType = BudgetType.CATEGORY,

    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,  // Null for overall budgets

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "period_type")
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,

    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,

    @ColumnInfo(name = "end_date")
    val endDate: LocalDate? = null,

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = true,

    // Alert thresholds
    @ColumnInfo(name = "alert_at_80")
    val alertAt80: Boolean = true,

    @ColumnInfo(name = "alert_at_100")
    val alertAt100: Boolean = true,

    @ColumnInfo(name = "alert_over_budget")
    val alertOverBudget: Boolean = true,

    @ColumnInfo(name = "enable_notifications")
    val enableNotifications: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "remote_id")
    val remoteId: Int? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)

/**
 * Budget Alert Type
 */
enum class BudgetAlertType {
    THRESHOLD_80,
    THRESHOLD_100,
    OVER_BUDGET
}

/**
 * Budget Alert Entity - Tracks when budget alerts are triggered
 */
@Entity(
    tableName = "budget_alerts",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("budget_id")]
)
data class BudgetAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "budget_id")
    val budgetId: Int,

    @ColumnInfo(name = "alert_type")
    val alertType: BudgetAlertType,

    @ColumnInfo(name = "triggered_at")
    val triggeredAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "is_dismissed")
    val isDismissed: Boolean = false,

    @ColumnInfo(name = "spent_amount")
    val spentAmount: Double,

    @ColumnInfo(name = "budget_amount")
    val budgetAmount: Double
)
