package com.example.spenttracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Category Entity for Room Database
 * Enhanced with sync tracking fields like ExpenseEntity
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = false) // Don't auto-generate - we control the IDs
    val id: Long, // Use server ID directly (negative for local-only categories)
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "color")
    val color: String, // Hex color code like "#3B82F6"
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Sync tracking fields - Simplified without serverId
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = CategorySyncStatus.PENDING.name,  // Like Laravel's job status
    
    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = true,  // Like Laravel's isDirty() check
    
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: String? = null  // Like Laravel's sync timestamp
)

/**
 * Category sync status enum - Similar to expense sync status
 */
enum class CategorySyncStatus {
    PENDING,     // Like Laravel's 'queued' - waiting to sync
    SYNCING,     // Like Laravel's 'processing' - currently syncing  
    SYNCED,      // Like Laravel's 'completed' - successfully synced
    FAILED,      // Like Laravel's 'failed' - sync error occurred
    CONFLICT,    // Like Laravel's 'retry' - server conflict needs resolution
    DELETED      // Like Laravel's 'soft deleted' - marked for deletion
}