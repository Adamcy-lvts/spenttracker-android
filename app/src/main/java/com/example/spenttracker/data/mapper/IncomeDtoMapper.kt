package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.remote.dto.IncomeDto
import com.example.spenttracker.data.remote.dto.CreateIncomeRequest
import com.example.spenttracker.data.remote.dto.UpdateIncomeRequest
import com.example.spenttracker.domain.model.Income
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mapper functions to convert between Income domain model and API DTOs
 */
class IncomeDtoMapper {

    /**
     * Convert IncomeDto from API to Income domain model
     */
    fun toDomain(dto: IncomeDto): Income {
        return Income(
            id = 0, // Local ID will be assigned by Room
            userId = dto.userId,
            source = dto.source,
            amount = dto.amount,
            date = LocalDate.parse(dto.date),
            categoryId = dto.categoryId?.toInt(),
            description = dto.description,
            isRecurring = dto.isRecurring,
            recurrenceType = dto.recurrenceType?.let {
                com.example.spenttracker.data.local.entity.RecurrenceType.valueOf(it.uppercase())
            },
            createdAt = dto.createdAt?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                    LocalDateTime.now()
                }
            } ?: LocalDateTime.now(),
            updatedAt = dto.updatedAt?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                    LocalDateTime.now()
                }
            } ?: LocalDateTime.now(),
            remoteId = dto.id,
            isSynced = true
        )
    }

    /**
     * Convert Income domain model to CreateIncomeRequest
     */
    fun toCreateRequest(income: Income): CreateIncomeRequest {
        return CreateIncomeRequest(
            source = income.source,
            amount = income.amount,
            date = income.date.toString(), // ISO format (YYYY-MM-DD)
            categoryId = income.categoryId?.toLong(),
            description = income.description,
            isRecurring = income.isRecurring,
            recurrenceType = income.recurrenceType?.name?.lowercase()
        )
    }

    /**
     * Convert Income domain model to UpdateIncomeRequest
     */
    fun toUpdateRequest(income: Income): UpdateIncomeRequest {
        return UpdateIncomeRequest(
            source = income.source,
            amount = income.amount,
            date = income.date.toString(),
            categoryId = income.categoryId?.toLong(),
            description = income.description,
            isRecurring = income.isRecurring,
            recurrenceType = income.recurrenceType?.name?.lowercase()
        )
    }

    /**
     * Convert list of IncomeDtos to list of Income domain models
     */
    fun toDomainList(dtos: List<IncomeDto>): List<Income> {
        return dtos.map { toDomain(it) }
    }
}
