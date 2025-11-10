package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.entity.CategoryEntity
import com.example.spenttracker.data.local.entity.CategorySyncStatus
import com.example.spenttracker.data.remote.dto.*
import com.example.spenttracker.domain.model.Category
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for Category DTOs - Converts between API DTOs and Domain/Entity models
 * Like Laravel's Resource/Transformer classes
 */
@Singleton
class CategoryDtoMapper @Inject constructor() {
    
    /**
     * Convert CategoryDto from API to CategoryEntity for database
     * Uses server ID directly as primary key
     */
    fun toEntity(dto: CategoryDto): CategoryEntity {
        return CategoryEntity(
            id = dto.id, // Use server ID directly as primary key
            name = dto.name,
            color = dto.color,
            description = dto.description,
            isActive = dto.isActive,
            icon = dto.icon,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = CategorySyncStatus.SYNCED.name,
            needsSync = false,
            lastSyncAt = Instant.now().toString()
        )
    }
    
    /**
     * Convert Category domain model to CreateCategoryRequest for API
     */
    fun toCreateRequest(category: Category): CreateCategoryRequest {
        return CreateCategoryRequest(
            name = category.name,
            color = category.color,
            icon = category.icon
        )
    }
    
    /**
     * Convert Category domain model to UpdateCategoryRequest for API
     */
    fun toUpdateRequest(category: Category): UpdateCategoryRequest {
        return UpdateCategoryRequest(
            name = category.name,
            color = category.color,
            icon = category.icon
        )
    }
    
    /**
     * Convert CategoryDto to Category domain model
     */
    fun toDomain(dto: CategoryDto): Category {
        return Category(
            id = dto.id.toInt(), // Convert server ID to int for domain
            name = dto.name,
            color = dto.color,
            description = dto.description,
            isActive = dto.isActive,
            icon = dto.icon,
            expenseCount = 0 // Will be calculated separately if needed
        )
    }
}