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
    
    /**
     * Add new category
     */
    suspend fun addCategory(category: Category): Long
    
    /**
     * Update existing category
     */
    suspend fun updateCategory(category: Category)
    
    /**
     * Delete category by ID
     */
    suspend fun deleteCategory(id: Int)
    
    /**
     * Toggle category active status
     */
    suspend fun toggleCategoryStatus(id: Int)
}