package com.example.spenttracker.presentation.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.domain.model.Income
import com.example.spenttracker.presentation.theme.ShadcnButton
import com.example.spenttracker.presentation.theme.ShadcnButtonSize
import com.example.spenttracker.presentation.theme.ShadcnButtonVariant
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Income management screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    onMenuClick: () -> Unit = {},
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val allIncomes by viewModel.allIncomes.collectAsState()
    val isLoading by viewModel.isAddingIncome.collectAsState()
    val deletingId by viewModel.deletingIncomeId.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<Income?>(null) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is IncomeEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is IncomeEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.error)
                }
                is IncomeEvent.IncomeAdded, is IncomeEvent.IncomeUpdated -> {
                    showAddDialog = false
                    incomeToEdit = null
                }
                is IncomeEvent.IncomeDeleted -> {
                    incomeToDelete = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Income")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Income list
            when (state) {
                is IncomeListState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is IncomeListState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((state as IncomeListState.Error).message)
                    }
                }
                is IncomeListState.Success -> {
                    if (allIncomes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No income yet")
                                Spacer(modifier = Modifier.height(8.dp))
                                ShadcnButton(
                                    onClick = { showAddDialog = true },
                                    variant = ShadcnButtonVariant.Default,
                                    size = ShadcnButtonSize.Default
                                ) {
                                    Text("Add Income")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total card
                            item {
                                val totalIncome = allIncomes.sumOf { it.amount }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Total Income",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "₦${String.format(Locale.getDefault(), "%,.2f", totalIncome)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Income items
                            items(allIncomes) { income ->
                                IncomeListItem(
                                    income = income,
                                    onEdit = {
                                        incomeToEdit = income
                                        showAddDialog = true
                                    },
                                    onDelete = { incomeToDelete = income },
                                    isDeleting = deletingId == income.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showAddDialog) {
        IncomeDialog(
            onDismiss = {
                showAddDialog = false
                incomeToEdit = null
            },
            onSave = { income ->
                if (incomeToEdit == null) {
                    viewModel.addIncome(income)
                } else {
                    viewModel.updateIncome(income)
                }
            },
            existingIncome = incomeToEdit,
            isLoading = isLoading
        )
    }

    // Delete confirmation
    incomeToDelete?.let { income ->
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text("Delete Income") },
            text = { Text("Are you sure you want to delete this income: ${income.source}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIncome(income.id)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun IncomeListItem(
    income: Income,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    income.source,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    income.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (income.description.isNotBlank()) {
                    Text(
                        income.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (income.isRecurring) {
                    Text(
                        income.getRecurrenceText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    income.getFormattedAmount(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onEdit, enabled = !isDeleting) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, enabled = !isDeleting) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
