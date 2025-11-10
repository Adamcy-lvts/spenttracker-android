package com.example.spenttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Room database entity for expenses
 * This represents how expenses are stored in the SQLite database
 * 
 * Sync Status Fields (Like Laravel's sync tracking):
 * - serverId: The ID from Laravel API (null = not synced yet)
 * - syncStatus: Current sync state (like Laravel's job status)
 * - needsSync: Flag for pending changes (like Laravel's dirty flag)
 * - lastSyncAt: When last synced (like Laravel's updated_at for sync)
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: String,  // Store as ISO string (YYYY-MM-DD)
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,  // References CategoryEntity.id (server category ID)
    @ColumnInfo(name = "user_id")
    val userId: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    
    // Sync tracking fields - Like Laravel's sync metadata
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,  // Laravel API ID (null = local only)
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.PENDING.name,  // Like Laravel's job status
    
    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = true,  // Like Laravel's isDirty() check
    
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: String? = null  // Like Laravel's sync timestamp
)

/**
 * Sync status enum - Like Laravel's job status constants
 * 
 * Kotlin Syntax Explanation:
 * - enum class: Like PHP enum but type-safe
 * - Represents all possible sync states
 */
enum class SyncStatus {
    PENDING,     // Like Laravel's 'queued' - waiting to sync
    SYNCING,     // Like Laravel's 'processing' - currently syncing  
    SYNCED,      // Like Laravel's 'completed' - successfully synced
    FAILED,      // Like Laravel's 'failed' - sync error occurred
    CONFLICT,    // Like Laravel's 'retry' - server conflict needs resolution
    DELETED      // Like Laravel's 'soft deleted' - marked for deletion
}