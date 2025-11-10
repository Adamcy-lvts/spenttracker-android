package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.entity.ExpenseEntity
import com.example.spenttracker.domain.model.Expense
import java.time.LocalDate
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Mapper functions to convert between Entity and Domain models
 */

/**
 * Safely parse date string that might be in different formats
 * Handles both "YYYY-MM-DD" and "YYYY-MM-DDTHH:mm:ss.ssssssZ" formats
 */
private fun parseDate(dateString: String): LocalDate {
    return try {
        if (dateString.contains('T')) {
            // Parse ISO datetime and extract date part
            Instant.parse(dateString).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        } else {
            // Parse simple date format
            LocalDate.parse(dateString)
        }
    } catch (e: Exception) {
        // Fallback to current date if parsing fails
        LocalDate.now()
    }
}

/**
 * Convert ExpenseEntity (database) to Expense (domain)
 */
fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        description = description,
        amount = amount,
        date = parseDate(date),  // Convert date string back to LocalDate safely
        categoryId = categoryId?.toInt(), // Convert Long? to Int? for domain model
        userId = userId
    )
}

/**
 * Convert Expense (domain) to ExpenseEntity (database)
 * Includes sync tracking fields - Like Laravel's model with sync metadata
 */
fun Expense.toEntity(): ExpenseEntity {
    val now = Instant.now().toString()
    return ExpenseEntity(
        id = id,
        description = description,
        amount = amount,
        date = date.toString(),  // Convert LocalDate to ISO string
        categoryId = categoryId?.toLong(), // Convert Int? to Long? for entity
        userId = userId,
        createdAt = now,
        updatedAt = now,
        // Sync fields - new expenses need sync by default (like Laravel's dirty tracking)
        syncStatus = com.example.spenttracker.data.local.entity.SyncStatus.PENDING.name,
        needsSync = true, // New expense needs to be synced - like Laravel's isDirty()
        lastSyncAt = null // Never synced yet - like Laravel's null timestamp
    )
}

/**
 * Convert list of entities to domain objects
 */
fun List<ExpenseEntity>.toDomainList(): List<Expense> {
    return this.map { it.toDomain() }
}

/**
 * Convert ExpenseWithCategory to Expense domain model
 */
fun com.example.spenttracker.data.local.ExpenseDao.ExpenseWithCategory.toDomain(): Expense {
    return Expense(
        id = id,
        description = description,
        amount = amount,
        date = parseDate(date),
        categoryId = categoryId?.toInt(), // Convert Long? to Int? for domain model
        categoryName = categoryName,
        categoryColor = categoryColor,
        userId = userId
    )
}

/**
 * Convert list of ExpenseWithCategory to domain objects
 */
fun List<com.example.spenttracker.data.local.ExpenseDao.ExpenseWithCategory>.toDomainListWithCategories(): List<Expense> {
    return this.map { it.toDomain() }
}

