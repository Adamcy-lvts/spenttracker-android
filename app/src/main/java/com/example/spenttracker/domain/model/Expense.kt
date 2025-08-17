package com.example.spenttracker.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.NumberFormat

/**
 * Currency formatting utility
 */
fun formatCurrency(amount: Double): String {
    return "₦${String.format(Locale.getDefault(), "%,.2f", amount)}"
}

/**
 * Domain model for Expense
 * This represents an expense in the business logic layer
 */
data class Expense(
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val categoryId: Int? = null,  // Optional category assignment
    val categoryName: String? = null,  // Category name for display
    val categoryColor: String? = null,  // Category color for display
    val userId: Int = 0
) {
    /**
     * Format amount for display
     */
    fun getFormattedAmount(): String {
        return formatCurrency(amount)
    }
    
    /**
     * Format date for display
     */
    fun getFormattedDate(): String {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))
    }
}