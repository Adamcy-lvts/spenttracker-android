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
        id = this.id,
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
 */
fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        name = this.name,
        color = this.color,
        description = this.description,
        isActive = this.isActive,
        icon = this.icon,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Convert CategoryWithExpenseCount to Category domain model
 */
fun CategoryDao.CategoryWithExpenseCount.toDomain(): Category {
    return Category(
        id = this.id,
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