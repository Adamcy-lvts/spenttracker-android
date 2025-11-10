package com.example.spenttracker.data.repository

import com.example.spenttracker.data.local.CategoryDao
import com.example.spenttracker.data.mapper.toDomain
import com.example.spenttracker.data.mapper.toDomainList
import com.example.spenttracker.data.mapper.toDomainWithCountList
import com.example.spenttracker.data.mapper.toEntity
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Category Repository Implementation
 * Handles all category data operations using Room database
 * Like Laravel Eloquent Repository
 */
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    
    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
            .map { entities -> entities.toDomainList() }
    }
    
    override suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllCategoriesList().toDomainList()
    }
    
    override fun getActiveCategories(): Flow<List<Category>> {
        return categoryDao.getActiveCategories()
            .map { entities -> entities.toDomainList() }
    }
    
    override fun getInactiveCategories(): Flow<List<Category>> {
        return categoryDao.getInactiveCategories()
            .map { entities -> entities.toDomainList() }
    }
    
    override fun getCategoriesWithExpenseCount(): Flow<List<Category>> {
        return categoryDao.getCategoriesWithExpenseCount()
            .map { entities -> entities.toDomainWithCountList() }
    }
    
    override suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id.toLong())?.toDomain()
    }
    
    // Categories are now read-only for users (managed globally by admin)
    // Removed: addCategory, updateCategory, deleteCategory, toggleCategoryStatus
    
    /**
     * Internal method for sync operations - updates categories from server
     */
    internal suspend fun syncCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }
    
    /**
     * Internal method for sync operations - updates category from server
     */
    internal suspend fun updateCategoryFromServer(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }
}