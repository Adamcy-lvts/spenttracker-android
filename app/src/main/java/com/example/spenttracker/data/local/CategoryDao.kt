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
    
    @Query("SELECT * FROM categories WHERE sync_status != 'DELETED' ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE sync_status != 'DELETED' ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE is_active = 1 AND sync_status != 'DELETED' ORDER BY name ASC")
    fun getActiveCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_active = 0 AND sync_status != 'DELETED' ORDER BY name ASC")
    fun getInactiveCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?
    
    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
    
    @Query("""
        SELECT c.*, 
        (SELECT COUNT(*) FROM expenses e WHERE e.category_id = c.id AND e.sync_status != 'DELETED') as expense_count
        FROM categories c 
        WHERE c.sync_status != 'DELETED'
        ORDER BY c.name ASC
    """)
    fun getCategoriesWithExpenseCount(): Flow<List<CategoryWithExpenseCount>>
    
    @Query("""
        SELECT c.*, 
        (SELECT COUNT(*) FROM expenses e WHERE e.category_id = c.id AND e.user_id = :userId AND e.sync_status != 'DELETED') as expense_count
        FROM categories c 
        WHERE c.sync_status != 'DELETED'
        ORDER BY c.name ASC
    """)
    fun getCategoriesWithExpenseCountForUser(userId: Long): Flow<List<CategoryWithExpenseCount>>
    
    // Sync-related queries - Like Laravel's sync database operations
    
    /**
     * Get categories that need to be synced to server
     */
    @Query("SELECT * FROM categories WHERE needs_sync = 1")
    suspend fun getCategoriesNeedingSync(): List<CategoryEntity>
    
    
    /**
     * Check if category name already exists (for duplicate prevention)
     */
    @Query("SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(:name) AND sync_status != 'DELETED' AND id != :excludeId")
    suspend fun countCategoriesWithName(name: String, excludeId: Long = -1): Int
    
    /**
     * Update category sync status
     */
    @Query("UPDATE categories SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String)
    
    /**
     * Update server info after successful sync
     */
    
    /**
     * Soft delete category by marking as deleted (for sync)
     */
    @Query("UPDATE categories SET sync_status = 'DELETED', needs_sync = 1 WHERE id = :id")
    suspend fun softDeleteCategory(id: Long)
    
    // Data class for category with expense count
    data class CategoryWithExpenseCount(
        val id: Long,
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