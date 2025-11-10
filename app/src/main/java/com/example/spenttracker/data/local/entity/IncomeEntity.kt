package com.example.spenttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Room database entity for incomes
 * This represents how incomes are stored in the SQLite database
 *
 * Sync Status Fields (Like Laravel's sync tracking):
 * - serverId: The ID from Laravel API (null = not synced yet)
 * - syncStatus: Current sync state (like Laravel's job status)
 * - needsSync: Flag for pending changes (like Laravel's dirty flag)
 * - lastSyncAt: When last synced (like Laravel's updated_at for sync)
 */
@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    val source: String,
    val amount: Double,
    val date: String,  // Store as ISO string (YYYY-MM-DD)

    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,  // Optional: Income category

    val description: String = "",

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String? = null,  // RecurrenceType enum name

    @ColumnInfo(name = "created_at")
    val createdAt: String = "",

    @ColumnInfo(name = "updated_at")
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
 * Recurrence type for recurring incomes
 */
enum class RecurrenceType {
    WEEKLY,      // Every week
    BIWEEKLY,    // Every 2 weeks
    MONTHLY,     // Every month (most common for salaries)
    QUARTERLY,   // Every 3 months
    YEARLY       // Every year (bonuses, etc.)
}
