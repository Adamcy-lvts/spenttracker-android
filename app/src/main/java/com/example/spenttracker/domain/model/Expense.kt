package com.example.spenttracker.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Domain model for Expense
 * This represents an expense in the business logic layer
 */
data class Expense(
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val userId: Int = 0
) {
    /**
     * Format amount for display
     */
    fun getFormattedAmount(): String {
        return "₦${String.format(Locale.getDefault(), "%.2f", amount)}"
    }
    
    /**
     * Format date for display
     */
    fun getFormattedDate(): String {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))
    }
}