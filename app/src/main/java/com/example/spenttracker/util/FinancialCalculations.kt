package com.example.spenttracker.util

import com.example.spenttracker.domain.model.*
import java.time.YearMonth

/**
 * Utility functions for financial calculations
 * Combines income and expense data for analytics
 */

/**
 * Calculate financial summary for a given period
 */
fun calculateFinancialSummary(
    incomes: List<Income>,
    expenses: List<Expense>,
    month: YearMonth
): FinancialSummary {
    // Filter for the selected month
    val monthIncomes = incomes.filter { YearMonth.from(it.date) == month }
    val monthExpenses = expenses.filter { YearMonth.from(it.date) == month }

    val totalIncome = monthIncomes.sumOf { it.amount }
    val totalExpenses = monthExpenses.sumOf { it.amount }
    val netIncome = totalIncome - totalExpenses

    val savingsRate = if (totalIncome > 0) {
        ((netIncome / totalIncome) * 100)
    } else 0.0

    val expenseRatio = if (totalIncome > 0) {
        ((totalExpenses / totalIncome) * 100)
    } else if (totalExpenses > 0) 100.0 else 0.0

    val status = when {
        savingsRate > 5 -> FinancialStatus.SURPLUS
        savingsRate < -5 -> FinancialStatus.DEFICIT
        else -> FinancialStatus.BALANCED
    }

    val incomeSourceBreakdown = calculateIncomeSourceBreakdown(monthIncomes)

    return FinancialSummary(
        period = month,
        totalIncome = totalIncome,
        totalExpenses = totalExpenses,
        netIncome = netIncome,
        savingsRate = savingsRate,
        expenseRatio = expenseRatio,
        incomeSourceBreakdown = incomeSourceBreakdown,
        status = status
    )
}

/**
 * Calculate income source breakdown
 */
fun calculateIncomeSourceBreakdown(incomes: List<Income>): List<IncomeSourceBreakdown> {
    val total = incomes.sumOf { it.amount }
    if (total == 0.0) return emptyList()

    return incomes
        .groupBy { it.source }
        .map { (source, incomeList) ->
            val amount = incomeList.sumOf { it.amount }
            IncomeSourceBreakdown(
                source = source,
                amount = amount,
                count = incomeList.size,
                percentage = (amount / total * 100)
            )
        }
        .sortedByDescending { it.amount }
}

/**
 * Calculate savings rate for a period
 */
fun calculateSavingsRate(totalIncome: Double, totalExpenses: Double): Double {
    return if (totalIncome > 0) {
        ((totalIncome - totalExpenses) / totalIncome * 100)
    } else 0.0
}

/**
 * Calculate expense ratio (expenses as percentage of income)
 */
fun calculateExpenseRatio(totalIncome: Double, totalExpenses: Double): Double {
    return if (totalIncome > 0) {
        (totalExpenses / totalIncome * 100)
    } else if (totalExpenses > 0) 100.0 else 0.0
}

/**
 * Get financial status based on savings rate
 */
fun getFinancialStatus(savingsRate: Double): FinancialStatus {
    return when {
        savingsRate > 5 -> FinancialStatus.SURPLUS
        savingsRate < -5 -> FinancialStatus.DEFICIT
        else -> FinancialStatus.BALANCED
    }
}

/**
 * Calculate average monthly income
 */
fun calculateAverageMonthlyIncome(incomes: List<Income>, monthsCount: Int = 6): Double {
    if (monthsCount <= 0 || incomes.isEmpty()) return 0.0

    val total = incomes.sumOf { it.amount }
    return total / monthsCount
}

/**
 * Calculate income vs expense comparison for multiple months
 */
data class MonthlyComparison(
    val month: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

fun calculateMonthlyComparison(
    incomes: List<Income>,
    expenses: List<Expense>,
    monthsCount: Int = 6
): List<MonthlyComparison> {
    val currentMonth = YearMonth.now()
    val comparisons = mutableListOf<MonthlyComparison>()

    for (i in (monthsCount - 1) downTo 0) {
        val month = currentMonth.minusMonths(i.toLong())

        val monthIncomes = incomes.filter { YearMonth.from(it.date) == month }
        val monthExpenses = expenses.filter { YearMonth.from(it.date) == month }

        val totalIncome = monthIncomes.sumOf { it.amount }
        val totalExpense = monthExpenses.sumOf { it.amount }
        val net = totalIncome - totalExpense

        comparisons.add(
            MonthlyComparison(
                month = month.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")),
                income = totalIncome,
                expense = totalExpense,
                net = net
            )
        )
    }

    return comparisons
}
