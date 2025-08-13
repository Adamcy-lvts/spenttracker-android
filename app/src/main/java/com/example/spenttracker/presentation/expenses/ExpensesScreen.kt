package com.example.spenttracker.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spenttracker.data.local.ExpenseDatabase
import com.example.spenttracker.data.repository.ExpenseRepositoryImpl
import com.example.spenttracker.domain.model.Expense
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple Expense data class for display
 */
data class SimpleExpense(
    val id: Int,
    val description: String,
    val amount: Double,
    val date: Date
) {
    fun getFormattedAmount(): String = "₦${String.format(Locale.getDefault(), "%.2f", amount)}"
    fun getFormattedDate(): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
}


/**
 * Simple Expenses Screen - Now with expense list display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToEditExpense: (Int) -> Unit = {}
) {
    // Create ViewModel with repository
    val context = LocalContext.current
    val database = ExpenseDatabase.getDatabase(context)
    val repository = ExpenseRepositoryImpl(database.expenseDao())
    val viewModel: ExpensesViewModel = viewModel { ExpensesViewModel(repository) }
    
    // Form state
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    // Delete confirmation state
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    
    // Edit dialog state
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    
    // Collect UI state from ViewModel
    val state by viewModel.state.collectAsState()
    val isAddingExpense by viewModel.isAddingExpense.collectAsState()
    val deletingExpenseId by viewModel.deletingExpenseId.collectAsState()
    
    // Handle events (like snackbars)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ExpenseListEvent.ShowSnackbar -> {
                    //  TODO: Show snackbar - for now just log
                    println("Snackbar: ${event.message}")
                }
                is ExpenseListEvent.NavigateToEdit -> {
                    onNavigateToEditExpense(event.expenseId)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Expense Tracker",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add Expense Form
            item {
                AddExpenseForm(
                    description = description,
                    amount = amount,
                    onDescriptionChange = { description = it },
                    onAmountChange = { amount = it },
                    onAddExpense = {
                        // Add expense using ViewModel
                        val amountValue = amount.toDoubleOrNull()
                        if (description.isNotBlank() && amountValue != null && amountValue > 0) {
                            viewModel.addExpense(description.trim(), amountValue)
                            description = ""
                            amount = ""
                        }
                    },
                    isLoading = isAddingExpense
                )
            }
            
            // Display expenses based on ViewModel state
            when (val currentState = state) {
                is ExpenseListState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                is ExpenseListState.Success -> {
                    // Expenses List Header
                    item {
                        Text(
                            text = "Recent Expenses",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    
                    if (currentState.expenses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No expenses yet. Add your first expense above!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Real expense list from database
                        items(
                            items = currentState.expenses,
                            key = { expense -> expense.id }
                        ) { expense ->
                            RealExpenseItem(
                                expense = expense,
                                onEditClick = { expenseToEdit = it },
                                onDeleteClick = { id ->
                                    // Show confirmation dialog
                                    expenseToDelete = expense
                                }
                            )
                        }
                    }
                }
                
                is ExpenseListState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Error loading expenses",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = currentState.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.refreshExpenses() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { 
                Text("Are you sure you want to delete \"${expense.description}\" (${expense.getFormattedAmount()})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense.id)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit expense dialog
    expenseToEdit?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            onDismiss = { expenseToEdit = null },
            onSave = { updatedExpense ->
                viewModel.updateExpense(updatedExpense)
                expenseToEdit = null
            }
        )
    }
}

/**
 * Add Expense Form Component
 * 
 * Reusable form component for adding new expenses
 * Like a Laravel form partial: @include('expenses._form')
 */
@Composable
fun AddExpenseForm(
    description: String,
    amount: String,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onAddExpense: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add New Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Description input field
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                placeholder = { Text("e.g., Lunch, Gas, Groceries") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Amount input field
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                leadingIcon = {
                    Text(
                        text = "₦",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            )
            
            // Add button
            Button(
                onClick = {
                    if (description.isNotBlank() && amount.isNotBlank() && !isLoading) {
                        onAddExpense()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() && amount.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adding...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense")
                }
            }
        }
    }
}

/**
 * RealExpenseItem - Individual expense display component for real Expense domain model
 */
@Composable
fun RealExpenseItem(
    expense: Expense,
    onEditClick: (Expense) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Expense details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = expense.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right side - Amount and action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = expense.getFormattedAmount(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Edit button
                IconButton(
                    onClick = { onEditClick(expense) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit expense",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = { onDeleteClick(expense.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete expense",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


/**
 * Edit Expense Dialog - Modal dialog for editing existing expenses
 */
@Composable
fun EditExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    // Dialog form state
    var description by remember { mutableStateOf(expense.description) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Lunch, Gas, Groceries") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    leadingIcon = {
                        Text(
                            text = "₦",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountValue != null && amountValue > 0) {
                        val updatedExpense = expense.copy(
                            description = description.trim(),
                            amount = amountValue
                        )
                        onSave(updatedExpense)
                    }
                },
                enabled = description.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Key Compose Concepts Explained:
 * 
 * 1. @Composable Functions:
 *    - Like Laravel Blade @component directives
 *    - Can contain other @Composable functions
 *    - Automatically re-render when data changes
 * 
 * 2. State Management:
 *    - collectAsState() = "Watch this data and update UI automatically"
 *    - Like Laravel's reactive data binding, but built-in
 * 
 * 3. Modifiers:
 *    - Like CSS classes: Modifier.fillMaxSize().padding(16.dp)
 *    - Chain multiple styling properties
 *    - Applied to UI components for styling and behavior
 * 
 * 4. Material Design:
 *    - MaterialTheme.typography.headlineSmall = Pre-defined text styles
 *    - MaterialTheme.colorScheme.error = Theme-aware colors
 *    - Cards, Buttons, Icons = Pre-built Material Design components
 * 
 * 5. Layout Composables:
 *    - Column = Vertical stack (like CSS flexbox column)
 *    - Row = Horizontal arrangement (like CSS flexbox row)  
 *    - Box = Single child container (like CSS relative positioning)
 *    - LazyColumn = Efficient scrolling list (like RecyclerView)
 */