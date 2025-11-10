package com.example.spenttracker.presentation.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.spenttracker.data.local.entity.RecurrenceType
import com.example.spenttracker.domain.model.Income
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Dialog for adding or editing income
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDialog(
    onDismiss: () -> Unit,
    onSave: (Income) -> Unit,
    existingIncome: Income? = null,
    isLoading: Boolean = false
) {
    var source by remember { mutableStateOf(existingIncome?.source ?: "") }
    var amount by remember { mutableStateOf(existingIncome?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(existingIncome?.description ?: "") }
    var selectedDate by remember { mutableStateOf(existingIncome?.date ?: LocalDate.now()) }
    var isRecurring by remember { mutableStateOf(existingIncome?.isRecurring ?: false) }
    var recurrenceType by remember { mutableStateOf(existingIncome?.recurrenceType) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }

    var sourceError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true

        if (source.isBlank()) {
            sourceError = "Source is required"
            isValid = false
        } else {
            sourceError = null
        }

        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            amountError = "Enter a valid amount"
            isValid = false
        } else {
            amountError = null
        }

        return isValid
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (existingIncome == null) "Add Income" else "Edit Income",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // Source field
                OutlinedTextField(
                    value = source,
                    onValueChange = {
                        source = it
                        sourceError = null
                    },
                    label = { Text("Source") },
                    placeholder = { Text("e.g., Salary, Freelance, Business") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = sourceError != null,
                    supportingText = sourceError?.let { { Text(it) } },
                    enabled = !isLoading
                )

                // Amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        amountError = null
                    },
                    label = { Text("Amount (₦)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    enabled = !isLoading
                )

                // Description field (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    maxLines = 3
                )

                // Recurring toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recurring Income", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        enabled = !isLoading
                    )
                }

                // Recurrence type dropdown (shown only if recurring)
                if (isRecurring) {
                    ExposedDropdownMenuBox(
                        expanded = showRecurrenceMenu,
                        onExpandedChange = { if (!isLoading) showRecurrenceMenu = it }
                    ) {
                        OutlinedTextField(
                            value = recurrenceType?.name?.lowercase()?.capitalize() ?: "Select",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recurrence Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRecurrenceMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isLoading
                        )
                        ExposedDropdownMenu(
                            expanded = showRecurrenceMenu,
                            onDismissRequest = { showRecurrenceMenu = false }
                        ) {
                            RecurrenceType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.lowercase().capitalize()) },
                                    onClick = {
                                        recurrenceType = type
                                        showRecurrenceMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (validate()) {
                                val income = Income(
                                    id = existingIncome?.id ?: 0,
                                    userId = existingIncome?.userId ?: 0,
                                    source = source.trim(),
                                    amount = amount.toDouble(),
                                    date = selectedDate,
                                    description = description.trim(),
                                    isRecurring = isRecurring,
                                    recurrenceType = if (isRecurring) recurrenceType else null,
                                    createdAt = existingIncome?.createdAt ?: LocalDateTime.now(),
                                    updatedAt = LocalDateTime.now()
                                )
                                onSave(income)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (existingIncome == null) "Add" else "Update")
                        }
                    }
                }
            }
        }
    }
}
