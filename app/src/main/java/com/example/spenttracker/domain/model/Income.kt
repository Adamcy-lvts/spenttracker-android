package com.example.spenttracker.domain.model

import com.example.spenttracker.data.local.entity.RecurrenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Income Domain Model
 * Represents income in the business logic layer
 */
data class Income(
    val id: Int = 0,
    val userId: Long,
    val source: String,
    val amount: Double,
    val date: LocalDate,
    val categoryId: Int? = null,
    val description: String = "",
    val isRecurring: Boolean = false,
    val recurrenceType: RecurrenceType? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val remoteId: Long? = null,
    val isSynced: Boolean = false
) {
    /**
     * Format amount as currency (Nigerian Naira)
     */
    fun getFormattedAmount(): String {
        return "₦${String.format(Locale.getDefault(), "%,.2f", amount)}"
    }

    /**
     * Format date for display
     */
    fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return date.format(formatter)
    }

    /**
     * Get recurrence display text
     */
    fun getRecurrenceText(): String {
        if (!isRecurring || recurrenceType == null) return ""
        return when (recurrenceType) {
            RecurrenceType.WEEKLY -> "Weekly"
            RecurrenceType.BIWEEKLY -> "Bi-weekly"
            RecurrenceType.MONTHLY -> "Monthly"
            RecurrenceType.QUARTERLY -> "Quarterly"
            RecurrenceType.YEARLY -> "Yearly"
        }
    }

    /**
     * Check if income is from current month
     */
    fun isCurrentMonth(): Boolean {
        val now = LocalDate.now()
        return date.year == now.year && date.month == now.month
    }
}

/**
 * Financial Summary combining income and expenses
 */
data class FinancialSummary(
    val period: java.time.YearMonth,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netIncome: Double,          // income - expenses
    val savingsRate: Double,         // (income - expenses) / income * 100
    val expenseRatio: Double,        // expenses / income * 100
    val incomeSourceBreakdown: List<IncomeSourceBreakdown>,
    val status: FinancialStatus
) {
    /**
     * Format currency values
     */
    fun getFormattedIncome(): String = "₦${String.format(Locale.getDefault(), "%,.2f", totalIncome)}"
    fun getFormattedExpenses(): String = "₦${String.format(Locale.getDefault(), "%,.2f", totalExpenses)}"
    fun getFormattedNetIncome(): String = "₦${String.format(Locale.getDefault(), "%,.2f", netIncome)}"
    fun getFormattedSavingsRate(): String = "${String.format("%.1f", savingsRate)}%"
    fun getFormattedExpenseRatio(): String = "${String.format("%.1f", expenseRatio)}%"
}

/**
 * Financial status based on savings rate
 */
enum class FinancialStatus {
    SURPLUS,     // income > expenses (savingsRate > 5%)
    BALANCED,    // income ≈ expenses (savingsRate between -5% and 5%)
    DEFICIT      // expenses > income (savingsRate < -5%)
}

/**
 * Income source breakdown for analytics
 */
data class IncomeSourceBreakdown(
    val source: String,
    val amount: Double,
    val count: Int,
    val percentage: Double
) {
    fun getFormattedAmount(): String = "₦${String.format(Locale.getDefault(), "%,.2f", amount)}"
    fun getFormattedPercentage(): String = "${String.format("%.1f", percentage)}%"
}
