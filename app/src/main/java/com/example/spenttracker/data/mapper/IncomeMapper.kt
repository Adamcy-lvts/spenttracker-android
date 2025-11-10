package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.entity.IncomeEntity
import com.example.spenttracker.data.local.entity.RecurrenceType
import com.example.spenttracker.data.local.entity.SyncStatus
import com.example.spenttracker.domain.model.Income
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mapper functions to convert between IncomeEntity (database) and Income (domain model)
 * Similar pattern to ExpenseMapper
 */

/**
 * Convert IncomeEntity to Income domain model
 */
fun IncomeEntity.toDomain(): Income {
    return Income(
        id = this.id,
        userId = this.userId,
        source = this.source,
        amount = this.amount,
        date = LocalDate.parse(this.date),
        categoryId = this.categoryId,
        description = this.description,
        isRecurring = this.isRecurring,
        recurrenceType = this.recurrenceType?.let { RecurrenceType.valueOf(it) },
        createdAt = if (this.createdAt.isNotEmpty()) {
            LocalDateTime.parse(this.createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } else {
            LocalDateTime.now()
        },
        updatedAt = if (this.updatedAt.isNotEmpty()) {
            LocalDateTime.parse(this.updatedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } else {
            LocalDateTime.now()
        },
        remoteId = this.serverId,
        isSynced = this.syncStatus == SyncStatus.SYNCED.name && !this.needsSync
    )
}

/**
 * Convert Income domain model to IncomeEntity
 */
fun Income.toEntity(): IncomeEntity {
    return IncomeEntity(
        id = this.id,
        userId = this.userId,
        source = this.source,
        amount = this.amount,
        date = this.date.toString(),  // Convert to ISO format (YYYY-MM-DD)
        categoryId = this.categoryId,
        description = this.description,
        isRecurring = this.isRecurring,
        recurrenceType = this.recurrenceType?.name,
        createdAt = this.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = this.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        serverId = this.remoteId,
        syncStatus = if (this.isSynced) SyncStatus.SYNCED.name else SyncStatus.PENDING.name,
        needsSync = !this.isSynced,
        lastSyncAt = if (this.isSynced) LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) else null
    )
}

/**
 * Convert list of IncomeEntity to list of Income
 */
fun List<IncomeEntity>.toDomainList(): List<Income> {
    return this.map { it.toDomain() }
}

/**
 * Convert list of Income to list of IncomeEntity
 */
fun List<Income>.toEntityList(): List<IncomeEntity> {
    return this.map { it.toEntity() }
}
