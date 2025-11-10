package com.example.spenttracker.data.mapper

import com.example.spenttracker.data.local.entity.ExpenseEntity
import com.example.spenttracker.data.local.entity.SyncStatus
import com.example.spenttracker.data.remote.dto.ExpenseDto
import com.example.spenttracker.data.remote.dto.CreateExpenseRequest
import com.example.spenttracker.data.remote.dto.UpdateExpenseRequest
import com.example.spenttracker.domain.model.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseDtoMapper @Inject constructor() {
    
    // Convert API response to domain model (like Laravel Resource)
    fun toDomainModel(dto: ExpenseDto): Expense {
        return Expense(
            id = dto.id.toInt(), // Convert Long to Int - like Laravel's (int) cast
            description = dto.description,
            amount = dto.amount,
            date = parseDate(dto.date),
            categoryId = dto.categoryId?.toInt(), // Convert nullable Long to nullable Int (for domain model)
            categoryName = dto.category?.name,
            categoryColor = dto.category?.color,
            userId = dto.userId.toInt()
        )
    }
    
    // Convert domain model to API request (like Laravel FormRequest)
    fun toDto(expense: Expense): ExpenseDto {
        return ExpenseDto(
            id = expense.id.toLong(), // Convert Int to Long - like Laravel's (string) cast
            description = expense.description,
            amount = expense.amount,
            date = formatDate(expense.date),
            categoryId = expense.categoryId?.toLong(), // Convert nullable Int to nullable Long
            userId = expense.userId.toLong()
        )
    }
    
    // Convert list of DTOs to domain models
    fun toDomainModelList(dtos: List<ExpenseDto>): List<Expense> {
        return dtos.map { toDomainModel(it) }
    }
    
    // Like Laravel Carbon::parse()
    private fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
    
    // Like Laravel Carbon::format()
    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Convert ExpenseDto to ExpenseEntity (for downloading from server)
     */
    fun toEntity(dto: ExpenseDto, userId: Long): ExpenseEntity {
        return ExpenseEntity(
            id = 0, // Let Room auto-generate local ID
            description = dto.description,
            amount = dto.amount,
            date = dto.date,
            categoryId = dto.categoryId, // Keep as Long to match ExpenseEntity
            userId = userId.toInt(),
            createdAt = dto.createdAt ?: Instant.now().toString(),
            updatedAt = dto.updatedAt ?: Instant.now().toString(),
            serverId = dto.id,
            syncStatus = SyncStatus.SYNCED.name,
            needsSync = false,
            lastSyncAt = Instant.now().toString()
        )
    }
    
    /**
     * Convert Expense domain model to CreateExpenseRequest for API
     */
    fun toCreateRequest(expense: Expense): CreateExpenseRequest {
        return CreateExpenseRequest(
            description = expense.description,
            amount = expense.amount,
            date = formatDate(expense.date),
            categoryId = expense.categoryId?.toLong()
        )
    }
    
    /**
     * Convert Expense domain model to UpdateExpenseRequest for API
     */
    fun toUpdateRequest(expense: Expense): UpdateExpenseRequest {
        return UpdateExpenseRequest(
            description = expense.description,
            amount = expense.amount,
            date = formatDate(expense.date),
            categoryId = expense.categoryId?.toLong()
        )
    }
}