package com.example.spenttracker.util

import android.content.Context
import android.os.Environment
import com.example.spenttracker.domain.model.Expense
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Export formats supported
 */
enum class ExportFormat {
    CSV,
    JSON,
    TXT
}

/**
 * Expense Export Manager
 * Handles exporting expenses to various formats
 */
@Singleton
class ExpenseExportManager @Inject constructor(
    private val context: Context
) {

    /**
     * Export expenses to specified format
     */
    fun exportExpenses(
        expenses: List<Expense>,
        format: ExportFormat,
        fileName: String? = null
    ): Result<File> {
        return try {
            val file = createExportFile(format, fileName)

            when (format) {
                ExportFormat.CSV -> exportToCsv(expenses, file)
                ExportFormat.JSON -> exportToJson(expenses, file)
                ExportFormat.TXT -> exportToText(expenses, file)
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create export file in Downloads directory
     */
    private fun createExportFile(format: ExportFormat, fileName: String?): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val name = fileName ?: "expenses_export_$timestamp"
        val extension = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
            ExportFormat.TXT -> "txt"
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        return File(downloadsDir, "$name.$extension")
    }

    /**
     * Export to CSV format
     */
    private fun exportToCsv(expenses: List<Expense>, file: File) {
        FileWriter(file).use { writer ->
            // Header
            writer.append("Date,Description,Amount,Category\n")

            // Data rows
            expenses.forEach { expense ->
                writer.append("${expense.date},")
                writer.append("\"${expense.description}\",")
                writer.append("${expense.amount},")
                writer.append("\"${expense.categoryName ?: "Uncategorized"}\"\n")
            }
        }
    }

    /**
     * Export to JSON format
     */
    private fun exportToJson(expenses: List<Expense>, file: File) {
        FileWriter(file).use { writer ->
            writer.append("[\n")

            expenses.forEachIndexed { index, expense ->
                writer.append("  {\n")
                writer.append("    \"date\": \"${expense.date}\",\n")
                writer.append("    \"description\": \"${expense.description}\",\n")
                writer.append("    \"amount\": ${expense.amount},\n")
                writer.append("    \"category\": \"${expense.categoryName ?: "Uncategorized"}\",\n")
                writer.append("    \"categoryId\": ${expense.categoryId}\n")
                writer.append("  }")

                if (index < expenses.size - 1) {
                    writer.append(",")
                }
                writer.append("\n")
            }

            writer.append("]\n")
        }
    }

    /**
     * Export to plain text format
     */
    private fun exportToText(expenses: List<Expense>, file: File) {
        FileWriter(file).use { writer ->
            writer.append("EXPENSE REPORT\n")
            writer.append("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}\n")
            writer.append("Total Expenses: ${expenses.size}\n")
            writer.append("Total Amount: ₦${String.format("%,.2f", expenses.sumOf { it.amount })}\n")
            writer.append("\n")
            writer.append("=" .repeat(80))
            writer.append("\n\n")

            expenses.forEach { expense ->
                writer.append("Date: ${expense.getFormattedDate()}\n")
                writer.append("Description: ${expense.description}\n")
                writer.append("Amount: ${expense.getFormattedAmount()}\n")
                writer.append("Category: ${expense.categoryName ?: "Uncategorized"}\n")
                writer.append("\n")
                writer.append("-" .repeat(80))
                writer.append("\n\n")
            }
        }
    }

    /**
     * Get export statistics
     */
    fun getExportSummary(expenses: List<Expense>): ExportSummary {
        val total = expenses.sumOf { it.amount }
        val byCategory = expenses.groupBy { it.categoryName ?: "Uncategorized" }
            .mapValues { it.value.sumOf { expense -> expense.amount } }

        return ExportSummary(
            totalExpenses = expenses.size,
            totalAmount = total,
            dateRange = if (expenses.isNotEmpty()) {
                "${expenses.minOf { it.date }} to ${expenses.maxOf { it.date }}"
            } else {
                "N/A"
            },
            categorySummary = byCategory
        )
    }
}

/**
 * Export summary data
 */
data class ExportSummary(
    val totalExpenses: Int,
    val totalAmount: Double,
    val dateRange: String,
    val categorySummary: Map<String, Double>
)
