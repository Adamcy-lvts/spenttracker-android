package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.CategoryDao
import com.example.spenttracker.data.local.entity.CategoryEntity
import com.example.spenttracker.domain.model.Category

/**
 * Category Mappers - Convert between data and domain models
 * Like Laravel Model transformations
 */

/**
 * Convert CategoryEntity to Category domain model
 */
fun CategoryEntity.toDomain(): Category {
    return Category(
        id = this.id.toInt(), // Convert Long to Int for domain model
        name = this.name,
        color = this.color,
        description = this.description,
        isActive = this.isActive,
        icon = this.icon,
        expenseCount = 0 // Will be set separately when needed
    )
}

/**
 * Convert Category domain model to CategoryEntity
 * For new categories (id == 0), assigns temporary negative ID
 */
fun Category.toEntity(): CategoryEntity {
    // Generate temporary negative ID for new categories
    val categoryId = if (this.id == 0) {
        -(System.currentTimeMillis() % 1000000) // Temporary negative ID
    } else {
        this.id.toLong() // Use existing ID
    }
    
    return CategoryEntity(
        id = categoryId,
        name = this.name,
        color = this.color,
        description = this.description,
        isActive = this.isActive,
        icon = this.icon,
        updatedAt = System.currentTimeMillis(),
        // New categories need sync by default
        syncStatus = com.example.spenttracker.data.local.entity.CategorySyncStatus.PENDING.name,
        needsSync = true,
        lastSyncAt = null
    )
}

/**
 * Convert CategoryWithExpenseCount to Category domain model
 */
fun CategoryDao.CategoryWithExpenseCount.toDomain(): Category {
    return Category(
        id = this.id.toInt(), // Convert Long to Int for domain model
        name = this.name,
        color = this.color,
        description = this.description,
        isActive = this.isActive,
        icon = this.icon,
        expenseCount = this.expenseCount
    )
}

/**
 * Convert list of CategoryEntity to list of Category domain models
 */
fun List<CategoryEntity>.toDomainList(): List<Category> {
    return this.map { it.toDomain() }
}

/**
 * Convert list of CategoryWithExpenseCount to list of Category domain models
 */
fun List<CategoryDao.CategoryWithExpenseCount>.toDomainWithCountList(): List<Category> {
    return this.map { it.toDomain() }
}