package com.example.spenttracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Room database entity for expenses
 * This represents how expenses are stored in the SQLite database
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: String,  // Store as ISO string (YYYY-MM-DD)
    @ColumnInfo(name = "category_id")
    val categoryId: Int? = null,  // Optional category assignment
    val userId: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)