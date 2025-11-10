package com.example.spenttracker.domain.repository

import com.example.spenttracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Category Repository Interface
 * Defines contract for category data operations
 * Like Laravel Repository pattern
 */
interface CategoryRepository {
    
    /**
     * Get all categories (Flow for reactive updates)
     */
    fun getCategories(): Flow<List<Category>>
    
    /**
     * Get all categories (List for one-time checks)
     */
    suspend fun getAllCategories(): List<Category>
    
    /**
     * Get only active categories
     */
    fun getActiveCategories(): Flow<List<Category>>
    
    /**
     * Get only inactive categories
     */
    fun getInactiveCategories(): Flow<List<Category>>
    
    /**
     * Get categories with expense counts
     */
    fun getCategoriesWithExpenseCount(): Flow<List<Category>>
    
    /**
     * Get category by ID
     */
    suspend fun getCategoryById(id: Int): Category?
    
    // Categories are now read-only for users (managed globally by admin)
    // User-facing CRUD operations removed for consistent analytics tracking
}