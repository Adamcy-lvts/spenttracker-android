package com.example.spenttracker.presentation.budgets

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.domain.model.*
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Budget Screen - Main budget overview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onMenuClick: () -> Unit = {},
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BudgetEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is BudgetEvent.BudgetCreated -> {
                    showAddBudgetDialog = false
                }
                is BudgetEvent.BudgetUpdated -> {
                    budgetToEdit = null
                }
                is BudgetEvent.BudgetDeleted -> {
                    // Handled by state update
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budgets",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddBudgetDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Budget") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is BudgetUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is BudgetUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is BudgetUiState.Success -> {
                BudgetContent(
                    summary = state.summary,
                    selectedMonth = selectedMonth,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onResetToCurrentMonth = { viewModel.resetToCurrentMonth() },
                    onEditBudget = { budget -> budgetToEdit = budget },
                    onDeleteBudget = { budgetId -> viewModel.deleteBudget(budgetId) },
                    onDismissAlert = { alertId -> viewModel.dismissAlert(alertId) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Add/Edit Budget Dialog
    if (showAddBudgetDialog) {
        AddEditBudgetDialog(
            budget = null,
            categories = categories,
            isLoading = isCreating,
            onDismiss = { showAddBudgetDialog = false },
            onSave = { budget ->
                viewModel.createBudget(budget)
            }
        )
    }

    budgetToEdit?.let { budget ->
        AddEditBudgetDialog(
            budget = budget,
            categories = categories,
            isLoading = isCreating,
            onDismiss = { budgetToEdit = null },
            onSave = { updatedBudget ->
                viewModel.updateBudget(updatedBudget)
            }
        )
    }
}

/**
 * Budget Content - Main content area
 */
@Composable
private fun BudgetContent(
    summary: BudgetSummary,
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit,
    onEditBudget: (Budget) -> Unit,
    onDeleteBudget: (Int) -> Unit,
    onDismissAlert: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Navigation
        item {
            MonthNavigationCard(
                selectedMonth = selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetToCurrentMonth = onResetToCurrentMonth
            )
        }

        // Active Alerts
        if (summary.hasAlerts) {
            items(summary.alerts.filter { !it.isDismissed }) { alert ->
                BudgetAlertCard(
                    alert = alert,
                    onDismiss = { onDismissAlert(alert.id) }
                )
            }
        }

        // Overall Budget (if exists)
        summary.overallBudget?.let { overallBudget ->
            item {
                Text(
                    text = "Overall Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                CategoryBudgetCard(
                    budgetWithSpending = overallBudget,
                    onEdit = { onEditBudget(overallBudget.budget) },
                    onDelete = { onDeleteBudget(overallBudget.budget.id) }
                )
            }
        }

        // Budget Summary
        if (summary.hasBudgets) {
            item {
                BudgetSummaryCard(summary = summary)
            }
        }

        // Category Budgets
        if (summary.categoryBudgets.isNotEmpty()) {
            item {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(summary.categoryBudgets) { budgetWithSpending ->
                CategoryBudgetCard(
                    budgetWithSpending = budgetWithSpending,
                    onEdit = { onEditBudget(budgetWithSpending.budget) },
                    onDelete = { onDeleteBudget(budgetWithSpending.budget.id) }
                )
            }
        } else if (summary.overallBudget == null) {
            // Only show empty state if no overall budget exists either
            item {
                EmptyBudgetsCard()
            }
        }

        // Unbudgeted Expenses
        if (summary.unbudgetedSpent > 0) {
            item {
                UnbudgetedExpensesCard(amount = summary.unbudgetedSpent)
            }
        }
    }
}

/**
 * Format currency
 */
private fun formatCurrency(amount: Double): String {
    return "₦${String.format(Locale.getDefault(), "%,.2f", amount)}"
}
