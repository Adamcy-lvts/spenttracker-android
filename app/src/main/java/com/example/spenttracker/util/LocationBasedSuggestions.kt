package com.example.spenttracker.util

import android.util.Log
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.domain.model.Expense
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location-based suggestions and analytics utility
 * Provides intelligent suggestions based on user location and spending patterns
 */
@Singleton
class LocationBasedSuggestions @Inject constructor() {
    
    companion object {
        private const val TAG = "LocationSuggestions"
    }
    
    /**
     * Get spending suggestions based on current location
     */
    fun getLocationBasedSuggestions(
        currentLocation: LocationData?,
        userExpenses: List<Expense>,
        categories: List<Category>
    ): List<LocationSuggestion> {
        if (currentLocation == null) return emptyList()
        
        val suggestions = mutableListOf<LocationSuggestion>()
        
        // Analyze spending patterns by location components
        val locationAnalysis = analyzeSpendingByLocation(userExpenses)
        
        // Check for neighborhood-based suggestions
        currentLocation.subLocality?.let { neighborhood ->
            locationAnalysis.neighborhoodPatterns[neighborhood]?.let { pattern ->
                suggestions.add(
                    LocationSuggestion(
                        type = SuggestionType.NEIGHBORHOOD_PATTERN,
                        title = "Spending Pattern Alert",
                        message = "You usually spend ₦${pattern.averageAmount} on ${pattern.topCategory} in $neighborhood",
                        location = neighborhood,
                        confidence = pattern.confidence
                    )
                )
            }
        }
        
        // Check for area-based shopping suggestions
        suggestions.addAll(getShoppingSuggestions(currentLocation, userExpenses))
        
        // Check for budget alerts based on location
        suggestions.addAll(getBudgetAlerts(currentLocation, userExpenses))
        
        return suggestions.sortedByDescending { it.confidence }
    }
    
    /**
     * Analyze spending patterns by location
     */
    private fun analyzeSpendingByLocation(expenses: List<Expense>): LocationAnalysis {
        val neighborhoodPatterns = mutableMapOf<String, SpendingPattern>()
        val cityPatterns = mutableMapOf<String, SpendingPattern>()
        
        // Group expenses by location (this would need location data in Expense model)
        // For now, we'll create a basic analysis structure
        
        return LocationAnalysis(
            neighborhoodPatterns = neighborhoodPatterns,
            cityPatterns = cityPatterns
        )
    }
    
    /**
     * Get shopping suggestions based on location
     */
    private fun getShoppingSuggestions(
        location: LocationData,
        expenses: List<Expense>
    ): List<LocationSuggestion> {
        val suggestions = mutableListOf<LocationSuggestion>()
        
        // Known shopping areas in Maiduguri (example)
        val shoppingAreas = mapOf(
            "Kantin Kwari" to listOf("Shopping", "Food & Dining"),
            "Monday Market" to listOf("Shopping", "Food & Dining"),
            "Maiduguri Mall" to listOf("Shopping", "Entertainment"),
            "Custom Market" to listOf("Shopping"),
            "GRA" to listOf("Food & Dining", "Entertainment")
        )
        
        // Check if user is near a shopping area
        location.subLocality?.let { area ->
            shoppingAreas.forEach { (shopArea, categories) ->
                if (area.contains(shopArea, ignoreCase = true) || 
                    location.displayLocation.contains(shopArea, ignoreCase = true)) {
                    
                    suggestions.add(
                        LocationSuggestion(
                            type = SuggestionType.SHOPPING_OPPORTUNITY,
                            title = "Shopping Opportunity",
                            message = "You're near $shopArea - great for ${categories.joinToString(", ")}",
                            location = shopArea,
                            confidence = 0.8f
                        )
                    )
                }
            }
        }
        
        return suggestions
    }
    
    /**
     * Get budget alerts based on location and spending history
     */
    private fun getBudgetAlerts(
        location: LocationData,
        expenses: List<Expense>
    ): List<LocationSuggestion> {
        val suggestions = mutableListOf<LocationSuggestion>()
        
        // Analyze recent spending in this area
        val recentExpensesInArea = expenses.take(20) // Last 20 expenses as proxy
        val totalSpent = recentExpensesInArea.sumOf { it.amount }
        
        if (totalSpent > 10000) { // Example threshold
            suggestions.add(
                LocationSuggestion(
                    type = SuggestionType.BUDGET_ALERT,
                    title = "Spending Alert",
                    message = "You've spent ₦$totalSpent recently in this area. Consider reviewing your budget.",
                    location = location.city ?: "current area",
                    confidence = 0.7f
                )
            )
        }
        
        return suggestions
    }
    
    /**
     * Check if user is near a specific type of business
     */
    fun isNearBusinessType(location: LocationData, businessType: String): Boolean {
        // This could be enhanced with actual business location data
        val businessKeywords = when (businessType.lowercase()) {
            "shopping" -> listOf("market", "mall", "shop", "store", "plaza")
            "restaurant" -> listOf("restaurant", "eatery", "food", "kitchen")
            "fuel" -> listOf("fuel", "petrol", "gas", "station")
            "bank" -> listOf("bank", "atm", "financial")
            else -> emptyList()
        }
        
        return businessKeywords.any { keyword ->
            location.displayLocation.contains(keyword, ignoreCase = true) ||
            location.featureName?.contains(keyword, ignoreCase = true) == true
        }
    }
}

/**
 * Location-based suggestion data class
 */
data class LocationSuggestion(
    val type: SuggestionType,
    val title: String,
    val message: String,
    val location: String,
    val confidence: Float // 0.0 to 1.0
)

/**
 * Types of location-based suggestions
 */
enum class SuggestionType {
    NEIGHBORHOOD_PATTERN,
    SHOPPING_OPPORTUNITY,
    BUDGET_ALERT,
    CATEGORY_SUGGESTION,
    NEARBY_DEALS
}

/**
 * Location analysis data structures
 */
data class LocationAnalysis(
    val neighborhoodPatterns: Map<String, SpendingPattern>,
    val cityPatterns: Map<String, SpendingPattern>
)

data class SpendingPattern(
    val topCategory: String,
    val averageAmount: Double,
    val frequency: Int,
    val confidence: Float
)