package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.entity.ExpenseEntity
import com.example.spenttracker.domain.model.Expense
import java.time.LocalDate
import java.time.Instant

/**
 * Mapper functions to convert between Entity and Domain models
 */

/**
 * Convert ExpenseEntity (database) to Expense (domain)
 */
fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        description = description,
        amount = amount,
        date = LocalDate.parse(date),  // Convert ISO string back to LocalDate
        categoryId = categoryId,
        userId = userId
    )
}

/**
 * Convert Expense (domain) to ExpenseEntity (database)
 */
fun Expense.toEntity(): ExpenseEntity {
    val now = Instant.now().toString()
    return ExpenseEntity(
        id = id,
        description = description,
        amount = amount,
        date = date.toString(),  // Convert LocalDate to ISO string
        categoryId = categoryId,
        userId = userId,
        createdAt = now,
        updatedAt = now
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
        date = LocalDate.parse(date),
        categoryId = categoryId,
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

