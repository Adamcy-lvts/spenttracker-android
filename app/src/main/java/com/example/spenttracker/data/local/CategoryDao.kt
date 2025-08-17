package com.example.spenttracker.data.local

import androidx.room.*
import com.example.spenttracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Category Data Access Object
 * Handles all database operations for categories
 */
@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_active = 0 ORDER BY name ASC")
    fun getInactiveCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?
    
    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)
    
    @Query("""
        SELECT c.*, 
        (SELECT COUNT(*) FROM expenses e WHERE e.category_id = c.id) as expense_count
        FROM categories c 
        ORDER BY c.name ASC
    """)
    fun getCategoriesWithExpenseCount(): Flow<List<CategoryWithExpenseCount>>
    
    // Data class for category with expense count
    data class CategoryWithExpenseCount(
        val id: Int,
        val name: String,
        val color: String,
        val description: String?,
        @ColumnInfo(name = "is_active") val isActive: Boolean,
        val icon: String?,
        @ColumnInfo(name = "created_at") val createdAt: Long,
        @ColumnInfo(name = "updated_at") val updatedAt: Long,
        @ColumnInfo(name = "expense_count") val expenseCount: Int
    )
}