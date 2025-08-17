package com.example.spenttracker.domain.model

/**
 * Category Domain Model
 * Represents expense categories matching the Vue.js web app structure
 */
data class Category(
    val id: Int = 0,
    val name: String,
    val color: String, // Hex color code like "#3B82F6"
    val description: String? = null,
    val isActive: Boolean = true,
    val icon: String? = null, // Future: emoji or icon identifier
    val expenseCount: Int = 0 // Calculated field for display
) {
    /**
     * Get formatted color for display
     */
    fun getColorInt(): Int {
        return try {
            android.graphics.Color.parseColor(color)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#3B82F6") // Default blue
        }
    }
    
    /**
     * Get display text with expense count
     */
    fun getDisplayText(): String {
        val expenseText = if (expenseCount == 1) "expense" else "expenses"
        return "$name ($expenseCount $expenseText)"
    }
}