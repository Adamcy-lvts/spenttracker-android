package com.example.spenttracker.data.local

import androidx.room.TypeConverter
import com.example.spenttracker.data.local.entity.BudgetAlertType
import com.example.spenttracker.data.local.entity.BudgetPeriodType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room Type Converters
 * Handles conversion between complex types and database-compatible types
 */
class Converters {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ========== LocalDate Converters ==========

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it, dateFormatter) }
    }

    // ========== LocalDateTime Converters ==========

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }

    // ========== BudgetPeriodType Converters ==========

    @TypeConverter
    fun fromBudgetPeriodType(type: BudgetPeriodType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toBudgetPeriodType(typeName: String?): BudgetPeriodType? {
        return typeName?.let { BudgetPeriodType.valueOf(it) }
    }

    // ========== BudgetAlertType Converters ==========

    @TypeConverter
    fun fromBudgetAlertType(type: BudgetAlertType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toBudgetAlertType(typeName: String?): BudgetAlertType? {
        return typeName?.let { BudgetAlertType.valueOf(it) }
    }
}
