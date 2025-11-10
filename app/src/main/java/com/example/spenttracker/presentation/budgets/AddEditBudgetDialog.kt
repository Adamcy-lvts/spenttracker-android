package com.example.spenttracker.presentation.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.spenttracker.data.local.entity.BudgetPeriodType
import com.example.spenttracker.data.local.entity.BudgetType
import com.example.spenttracker.domain.model.Budget
import com.example.spenttracker.domain.model.Category
import java.time.LocalDate
import java.time.YearMonth

/**
 * Add/Edit Budget Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetDialog(
    budget: Budget?,
    categories: List<Category>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    val isEditMode = budget != null

    var budgetType by remember { mutableStateOf(budget?.budgetType ?: BudgetType.CATEGORY) }
    var selectedCategory by remember { mutableStateOf<Category?>(
        if (isEditMode && budget?.categoryId != null) {
            categories.find { it.id == budget.categoryId }
        } else null
    ) }
    var amount by remember { mutableStateOf(budget?.amount?.toString() ?: "") }
    var periodType by remember { mutableStateOf(budget?.periodType ?: BudgetPeriodType.MONTHLY) }
    var isRecurring by remember { mutableStateOf(budget?.isRecurring ?: true) }
    var alertAt80 by remember { mutableStateOf(budget?.alertAt80 ?: true) }
    var alertAt100 by remember { mutableStateOf(budget?.alertAt100 ?: true) }
    var alertOverBudget by remember { mutableStateOf(budget?.alertOverBudget ?: true) }
    var enableNotifications by remember { mutableStateOf(budget?.enableNotifications ?: true) }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (isEditMode) "Edit Budget" else "Create Budget",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Error message
                if (showError) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Budget Type Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Budget Type",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = budgetType == BudgetType.OVERALL,
                            onClick = {
                                budgetType = BudgetType.OVERALL
                                selectedCategory = null  // Clear category selection for overall budget
                            },
                            label = { Text("Overall Budget") },
                            modifier = Modifier.weight(1f),
                            enabled = !isEditMode  // Can't change budget type when editing
                        )
                        FilterChip(
                            selected = budgetType == BudgetType.CATEGORY,
                            onClick = { budgetType = BudgetType.CATEGORY },
                            label = { Text("Category Budget") },
                            modifier = Modifier.weight(1f),
                            enabled = !isEditMode  // Can't change budget type when editing
                        )
                    }
                }

                // Category Selector (only show for category budgets)
                if (budgetType == BudgetType.CATEGORY) {
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isEditMode // Can't change category when editing
                        )

                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Only allow numbers and decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = newValue
                        }
                    },
                    label = { Text("Budget Amount") },
                    prefix = { Text("₦") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Period Type
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Budget Period",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = periodType == BudgetPeriodType.MONTHLY,
                            onClick = { periodType = BudgetPeriodType.MONTHLY },
                            label = { Text("Monthly") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = periodType == BudgetPeriodType.CUSTOM,
                            onClick = { periodType = BudgetPeriodType.CUSTOM },
                            label = { Text("Custom") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Recurring Toggle (only for monthly)
                if (periodType == BudgetPeriodType.MONTHLY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recurring Monthly",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Auto-renew budget each month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                    }
                }

                Divider()

                // Alert Settings
                Text(
                    text = "Alert Settings",
                    style = MaterialTheme.typography.titleMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert at 80% of budget",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = alertAt80,
                            onCheckedChange = { alertAt80 = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert at 100% of budget",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = alertAt100,
                            onCheckedChange = { alertAt100 = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alert when over budget",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = alertOverBudget,
                            onCheckedChange = { alertOverBudget = it }
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Notifications",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Receive push notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableNotifications,
                            onCheckedChange = { enableNotifications = it }
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Validation
                            when {
                                budgetType == BudgetType.CATEGORY && selectedCategory == null -> {
                                    errorMessage = "Please select a category"
                                    showError = true
                                }
                                amount.isEmpty() || amount.toDoubleOrNull() == null -> {
                                    errorMessage = "Please enter a valid amount"
                                    showError = true
                                }
                                amount.toDoubleOrNull()!! <= 0 -> {
                                    errorMessage = "Budget amount must be greater than 0"
                                    showError = true
                                }
                                else -> {
                                    showError = false
                                    val currentMonth = YearMonth.now()

                                    val newBudget = Budget(
                                        id = budget?.id ?: 0,
                                        budgetType = budgetType,
                                        categoryId = if (budgetType == BudgetType.CATEGORY) selectedCategory?.id else null,
                                        categoryName = if (budgetType == BudgetType.CATEGORY) selectedCategory?.name ?: "" else "Overall Budget",
                                        categoryColor = if (budgetType == BudgetType.CATEGORY) selectedCategory?.color ?: "" else "",
                                        userId = budget?.userId ?: 0,
                                        amount = amount.toDouble(),
                                        periodType = periodType,
                                        startDate = currentMonth.atDay(1),
                                        endDate = if (periodType == BudgetPeriodType.MONTHLY) null else currentMonth.atEndOfMonth(),
                                        isRecurring = isRecurring,
                                        alertAt80 = alertAt80,
                                        alertAt100 = alertAt100,
                                        alertOverBudget = alertOverBudget,
                                        enableNotifications = enableNotifications
                                    )

                                    onSave(newBudget)
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isEditMode) "Update" else "Create")
                    }
                }
            }
        }
    }
}
