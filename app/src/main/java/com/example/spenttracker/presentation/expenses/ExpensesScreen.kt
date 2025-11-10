package com.example.spenttracker.presentation.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.domain.model.Expense
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.presentation.expenses.DateFilterType
import com.example.spenttracker.util.ExportFormat
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.example.spenttracker.presentation.theme.ShadcnButton
import com.example.spenttracker.presentation.theme.ShadcnTextField
import com.example.spenttracker.presentation.theme.ShadcnButtonVariant
import com.example.spenttracker.presentation.theme.ShadcnButtonSize
import com.example.spenttracker.data.sync.SyncScheduler
import com.example.spenttracker.presentation.dashboard.SyncSchedulerEntryPoint
import com.example.spenttracker.presentation.categories.CategoryViewModel
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Currency formatting utilities
 */
fun formatCurrency(amount: Double): String {
    return "₦${String.format(Locale.getDefault(), "%,.2f", amount)}"
}

fun formatCurrencyInput(input: String): String {
    val cleanString = input.replace("[₦,]".toRegex(), "")
    if (cleanString.isEmpty()) return ""
    
    val parsed = try {
        cleanString.toDouble()
    } catch (e: NumberFormatException) {
        return input
    }
    
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.format(parsed)
}

fun extractNumericValue(formattedInput: String): String {
    return formattedInput.replace("[₦,]".toRegex(), "")
}

/**
 * Simple Expense data class for display
 */
data class SimpleExpense(
    val id: Int,
    val description: String,
    val amount: Double,
    val date: Date
) {
    fun getFormattedAmount(): String = formatCurrency(amount)
    fun getFormattedDate(): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
}


/**
 * Simple Expenses Screen - Now with expense list display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onNavigateToEditExpense: (Int) -> Unit = {},
    darkTheme: Boolean = false,
    onDarkThemeToggle: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    // Use Hilt dependency injection - Like Laravel's constructor injection
    // @HiltViewModel automatically provides all dependencies
    val viewModel: ExpensesViewModel = hiltViewModel()
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    
    // Get sync scheduler for manual sync after adding/editing expenses
    val context = LocalContext.current
    val syncScheduler = remember { 
        val app = context.applicationContext as com.example.spenttracker.SpentTrackerApplication
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            app,
            SyncSchedulerEntryPoint::class.java
        ).syncScheduler()
    }
    
    // Get categories from CategoryViewModel
    val activeCategories by categoryViewModel.categories.collectAsState()

    // Month navigation state
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }

    // Form state is now managed inside the AddExpenseDialog

    // Delete confirmation state
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    
    // Edit dialog state
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    
    // Add expense dialog state
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    // Export dialog state
    var showExportDialog by remember { mutableStateOf(false) }

    // Snackbar host state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Coroutine scope for async operations
    val scope = rememberCoroutineScope()

    // Collect UI state from ViewModel
    val state by viewModel.state.collectAsState()
    val isAddingExpense by viewModel.isAddingExpense.collectAsState()
    val deletingExpenseId by viewModel.deletingExpenseId.collectAsState()
    
    // Collect pagination state
    val currentPage by viewModel.currentPage.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val pageSize by viewModel.pageSize.collectAsState()
    
    // Collect date filter state
    val selectedDate by viewModel.selectedDate.collectAsState()
    val showAllDates by viewModel.showAllDates.collectAsState()
    val dateFilterType by viewModel.dateFilterType.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()

    // Update expenses when selected month changes
    LaunchedEffect(selectedMonth) {
        viewModel.filterByMonth(selectedMonth)
    }
    
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
                },
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Export button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Expenses"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Expense")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            userScrollEnabled = true // Explicitly enable scrolling
        ) {
            // Month Navigation Header
            item {
                MonthNavigationHeader(
                    selectedMonth = selectedMonth,
                    onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                    onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
                    onResetToCurrentMonth = { selectedMonth = YearMonth.now() }
                )
            }

            // Date Filter
            item {
                DateFilterControls(
                    selectedDate = selectedDate,
                    dateFilterType = dateFilterType,
                    onDateSelected = { date -> viewModel.filterByDate(date) },
                    onShowAllDates = { viewModel.showAllExpenses() },
                    onTodaySelected = { viewModel.showTodayExpenses() },
                    onThisMonthSelected = { viewModel.showThisMonthExpenses() }
                )
            }
            
            // Expenses List Header
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            
            // Total sum display
            item {
                TotalSumCard(
                    filteredExpenses = filteredExpenses,
                    filterType = dateFilterType
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
                    
                    if (currentState.expenses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
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
                        // Expense table container with subtle background
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Column {
                                    // Expense rows
                                    currentState.expenses.forEachIndexed { index, expense ->
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
                        }
                        
                        // Add pagination controls for expense list
                        if (totalExpenses > 0) {
                            item {
                                PaginationControls(
                                    currentPage = currentPage,
                                    totalPages = viewModel.getTotalPages(),
                                    totalItems = totalExpenses,
                                    pageSize = pageSize,
                                    onPreviousPage = { viewModel.previousPage() },
                                    onNextPage = { viewModel.nextPage() },
                                    onPageSelected = { page -> viewModel.goToPage(page) },
                                    onPageSizeChanged = { newSize -> viewModel.updatePageSize(newSize) }
                                )
                            }
                        }
                        
                        // Total sum display below the list
                        item {
                            TotalSumCard(
                                filteredExpenses = filteredExpenses,
                                filterType = dateFilterType
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
                ShadcnButton(
                    onClick = {
                        viewModel.deleteExpense(expense.id)
                        expenseToDelete = null
                    },
                    variant = ShadcnButtonVariant.Destructive,
                    size = ShadcnButtonSize.Default
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                ShadcnButton(
                    onClick = { expenseToDelete = null },
                    variant = ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Default
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add expense dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            categories = activeCategories,
            onDismiss = { 
                showAddExpenseDialog = false
            },
            onSave = { newExpense ->
                viewModel.addExpense(
                    newExpense.description, 
                    newExpense.amount, 
                    newExpense.categoryId, 
                    newExpense.date
                )
                showAddExpenseDialog = false
            },
            isLoading = isAddingExpense
        )
    }
    
    // Edit expense dialog
    expenseToEdit?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            categories = activeCategories,
            onDismiss = { expenseToEdit = null },
            onSave = { updatedExpense ->
                viewModel.updateExpense(updatedExpense)
                expenseToEdit = null
            }
        )
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                scope.launch {
                    viewModel.exportExpenses(format).collect { result ->
                        result.fold(
                            onSuccess = { file ->
                                snackbarHostState.showSnackbar("Exported to: ${file.absolutePath}")
                                showExportDialog = false
                            },
                            onFailure = { error ->
                                snackbarHostState.showSnackbar("Export failed: ${error.message}")
                            }
                        )
                    }
                }
            }
        )
    }
}

/**
 * Add Expense Form Component
 * 
 * Reusable form component for adding new expenses
 * Using shadcn/ui inspired card layout
 */
@Composable
fun AddExpenseForm(
    description: String,
    amount: String,
    selectedCategory: Category?,
    selectedDate: LocalDate,
    categories: List<Category>,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Category?) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onAddExpense: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Add New Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Description input field
            ShadcnTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                placeholder = { 
                    Text(
                        "e.g., Lunch, Gas, Groceries",
                        style = MaterialTheme.typography.bodySmall
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Amount input field
            ShadcnTextField(
                value = if (amount.isNotEmpty()) formatCurrencyInput(amount) else "",
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                placeholder = { 
                    Text(
                        "0.00",
                        style = MaterialTheme.typography.bodySmall
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                leadingIcon = {
                    Text(
                        text = "₦",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            
            // Date picker field
            DatePickerField(
                selectedDate = selectedDate,
                onDateChange = onDateChange,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category dropdown
            CategoryDropdown(
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = onCategoryChange,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Add button
            ShadcnButton(
                onClick = {
                    if (description.isNotBlank() && amount.isNotBlank() && !isLoading) {
                        onAddExpense()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() && amount.isNotBlank() && !isLoading,
                variant = ShadcnButtonVariant.Default,
                size = ShadcnButtonSize.Default,
                icon = if (!isLoading) Icons.Default.Add else null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Adding..." else "Add Expense")
            }
        }
    }
}

/**
 * RealExpenseItem - Enhanced expense row with shadcn/ui table styling
 */
@Composable
fun RealExpenseItem(
    expense: Expense,
    onEditClick: (Expense) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Enhanced row with hover and transition effects
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Row click handler if needed */ }
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .padding(8.dp)
            .then(
                if (isHovered) {
                    Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Icon + Description section
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Color Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (expense.categoryColor != null) {
                            Color(android.graphics.Color.parseColor(expense.categoryColor))
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        CircleShape
                    )
            )
            
            // Description and Date
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.getFormattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Category name if available
                    expense.categoryName?.let { categoryName ->
                        Text(
                            text = "• $categoryName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // Center - Amount
        Text(
            text = expense.getFormattedAmount(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Right side - Actions dropdown menu
        ExpenseActionsDropdown(
            onEditClick = { onEditClick(expense) },
            onDeleteClick = { onDeleteClick(expense.id) }
        )
    }
    
    // Subtle divider between rows
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}



/**
 * Edit Expense Dialog - Modal dialog for editing existing expenses
 */
@Composable
fun EditExpenseDialog(
    expense: Expense,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    // Dialog form state
    var description by remember { mutableStateOf(expense.description) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == expense.categoryId })
    }
    var editSelectedDate by remember { mutableStateOf(expense.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description field
                ShadcnTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = {
                    Text(
                        "e.g., Lunch, Gas, Groceries",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount field
                ShadcnTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Only allow digits and single decimal point
                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                        // Prevent multiple decimal points
                        val decimalCount = filtered.count { it == '.' }
                        if (decimalCount <= 1) {
                            amount = filtered
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = {
                    Text(
                        "0.00",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    leadingIcon = {
                        Text(
                            text = "₦",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                
                // Date picker field
                DatePickerField(
                    selectedDate = editSelectedDate,
                    onDateChange = { editSelectedDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category dropdown
                CategoryDropdown(
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ShadcnButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountValue != null && amountValue > 0) {
                        val updatedExpense = expense.copy(
                            description = description.trim(),
                            amount = amountValue,
                            categoryId = selectedCategory?.id,
                            date = editSelectedDate
                        )
                        onSave(updatedExpense)
                    }
                },
                enabled = description.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0,
                variant = ShadcnButtonVariant.Default,
                size = ShadcnButtonSize.Default
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            ShadcnButton(
                onClick = onDismiss,
                variant = ShadcnButtonVariant.Outline,
                size = ShadcnButtonSize.Default
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Add Expense Dialog Component
 *
 * Modal dialog for adding new expenses with form validation
 */
@Composable
fun AddExpenseDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
    isLoading: Boolean = false
) {
    // Dialog form state
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Expense") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description field
                ShadcnTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = {
                        Text(
                            "e.g., Lunch, Gas, Groceries",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount field
                ShadcnTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Only allow digits and single decimal point
                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                        // Prevent multiple decimal points
                        val decimalCount = filtered.count { it == '.' }
                        if (decimalCount <= 1) {
                            amount = filtered
                        }
                    },
                    label = { Text("Amount") },
                    placeholder = {
                        Text(
                            "0.00",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    leadingIcon = {
                        Text(
                            text = "₦",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                
                // Date picker field
                DatePickerField(
                    selectedDate = selectedDate,
                    onDateChange = { selectedDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category dropdown
                CategoryDropdown(
                    selectedCategory = selectedCategory,
                    categories = categories,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ShadcnButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (description.isNotBlank() && amountValue != null && amountValue > 0) {
                        val newExpense = Expense(
                            id = 0, // Will be auto-generated
                            description = description.trim(),
                            amount = amountValue,
                            date = selectedDate,
                            categoryId = selectedCategory?.id
                        )
                        onSave(newExpense)
                    }
                },
                enabled = description.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0 && !isLoading,
                variant = ShadcnButtonVariant.Default,
                size = ShadcnButtonSize.Default
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Adding..." else "Add Expense")
            }
        },
        dismissButton = {
            ShadcnButton(
                onClick = onDismiss,
                variant = ShadcnButtonVariant.Outline,
                size = ShadcnButtonSize.Default
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Pagination Controls Component with Page Size Selector
 */
@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    pageSize: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onPageSizeChanged: (Int) -> Unit
) {
    val startItem = currentPage * pageSize + 1
    val endItem = minOf((currentPage + 1) * pageSize, totalItems)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Page size selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items per page:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                PageSizeSelector(
                    currentPageSize = pageSize,
                    onPageSizeChanged = onPageSizeChanged
                )
            }
            
            // Pagination info
            Text(
                text = "Showing $startItem-$endItem of $totalItems expenses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                ShadcnButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 0,
                    variant = ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Default
                ) {
                    Text("Previous")
                }
                
                // Page indicator
                Text(
                    text = "Page ${currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Next button
                ShadcnButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages - 1,
                    variant = ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Default
                ) {
                    Text("Next")
                }
            }
            
            // Page dots indicator (for small number of pages)
            if (totalPages <= 5) {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalPages) { page ->
                        val isCurrentPage = page == currentPage
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isCurrentPage) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    shape = CircleShape
                                )
                                .clickable { onPageSelected(page) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page Size Selector Component
 */
@Composable
fun PageSizeSelector(
    currentPageSize: Int,
    onPageSizeChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val pageSizeOptions = listOf(5, 10, 15, 20)
    
    Box {
        ShadcnButton(
            onClick = { expanded = true },
            variant = ShadcnButtonVariant.Outline,
            size = ShadcnButtonSize.Small
        ) {
            Text("$currentPageSize")
            Spacer(modifier = Modifier.width(4.dp))
            Text("▼", style = MaterialTheme.typography.bodySmall)
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            pageSizeOptions.forEach { size ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "$size per page",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    onClick = {
                        onPageSizeChanged(size)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = if (size == currentPageSize) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }
    }
}

/**
 * Month Navigation Header Component
 */
@Composable
fun MonthNavigationHeader(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: () -> Unit
) {
    val isCurrentMonth = selectedMonth == YearMonth.now()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Month Button
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Month Display with Reset Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (!isCurrentMonth) {
                    TextButton(
                        onClick = onResetToCurrentMonth,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Current Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Next Month Button
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Date Filter Controls Component
 */
@Composable
fun DateFilterControls(
    selectedDate: LocalDate,
    dateFilterType: DateFilterType,
    onDateSelected: (LocalDate) -> Unit,
    onShowAllDates: () -> Unit,
    onTodaySelected: () -> Unit,
    onThisMonthSelected: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter header
            Text(
                text = "Filter by Date",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Filter buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All dates button
                ShadcnButton(
                    onClick = onShowAllDates,
                    variant = if (dateFilterType == DateFilterType.ALL) ShadcnButtonVariant.Default else ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Small
                ) {
                    Text("All")
                }
                
                // This Month button (default)
                ShadcnButton(
                    onClick = onThisMonthSelected,
                    variant = if (dateFilterType == DateFilterType.THIS_MONTH) ShadcnButtonVariant.Default else ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Small
                ) {
                    Text("This Month")
                }
                
                // Today button
                ShadcnButton(
                    onClick = onTodaySelected,
                    variant = if (dateFilterType == DateFilterType.TODAY) 
                        ShadcnButtonVariant.Default else ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Small
                ) {
                    Text("Today")
                }
                
                // Custom date picker button
                ShadcnButton(
                    onClick = { showDatePicker = true },
                    variant = if (dateFilterType == DateFilterType.CUSTOM_DATE) 
                        ShadcnButtonVariant.Default else ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Small
                ) {
                    Text("Pick Date")
                }
            }
            
            // Current filter info
            if (dateFilterType != DateFilterType.ALL) {
                val filterText = when (dateFilterType) {
                    DateFilterType.TODAY -> {
                        val todayFormatted = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        "Showing expenses for: $todayFormatted"
                    }
                    DateFilterType.THIS_MONTH -> {
                        val currentMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                        "Showing expenses for: $currentMonth"
                    }
                    DateFilterType.CUSTOM_DATE -> "Showing expenses for: ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                    else -> ""
                }
                if (filterText.isNotEmpty()) {
                    Text(
                        text = filterText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            initialDate = selectedDate
        )
    }
}

/**
 * Total Sum Card Component
 * 
 * Displays the total amount for the current filter
 */
@Composable
fun TotalSumCard(
    filteredExpenses: List<Expense>,
    filterType: DateFilterType
) {
    val totalAmount = filteredExpenses.sumOf { it.amount }
    
    val filterTitle = when (filterType) {
        DateFilterType.ALL -> "Total (All Time)"
        DateFilterType.TODAY -> {
            val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            "Total ($todayFormatted)"
        }
        DateFilterType.THIS_MONTH -> {
            val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            "Total ($currentMonth)"
        }
        DateFilterType.CUSTOM_DATE -> "Total (Selected Date)"
        DateFilterType.CUSTOM_MONTH -> "Total (Selected Month)"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = filterTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatCurrency(totalAmount),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${filteredExpenses.size} expense${if (filteredExpenses.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Date Picker Dialog Component
 */
@Composable
fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate
) {
    val context = LocalContext.current
    
    // Convert LocalDate to milliseconds for Android DatePicker
    val calendar = java.util.Calendar.getInstance()
    calendar.set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    val initialDateMillis = calendar.timeInMillis
    
    // Create and show Android DatePickerDialog
    LaunchedEffect(Unit) {
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )
        
        datePickerDialog.setOnDismissListener { onDismiss() }
        datePickerDialog.show()
    }
}

/**
 * Expense Actions Dropdown Component
 * Professional dropdown menu with 3 dots (⋮) button
 */
@Composable
fun ExpenseActionsDropdown(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        // 3 vertical dots button
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Edit option
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onClick = {
                    onEditClick()
                    expanded = false
                }
            )
            
            // Delete option
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                onClick = {
                    onDeleteClick()
                    expanded = false
                }
            )
        }
    }
}

/**
 * Category Dropdown Component
 * Styled dropdown for category selection matching shadcn/ui design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: Category?,
    categories: List<Category>,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = "Category (optional)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            ShadcnTextField(
                value = selectedCategory?.name ?: "Select category...",
                onValueChange = { /* Read-only */ },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // "No category" option
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "No category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onCategorySelected(null)
                        expanded = false
                    }
                )
                
                // Category options
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Color indicator
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            Color(category.getColorInt()),
                                            CircleShape
                                        )
                                )
                                
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Date Picker Field Component
 * 
 * Material Design 3 date picker with field-like appearance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Format the date for display
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val displayDate = selectedDate.format(dateFormatter)
    
    // Date picker field that looks like other form fields
    OutlinedTextField(
        value = displayDate,
        onValueChange = { }, // Read-only field
        label = { Text("Date") },
        modifier = modifier,
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        },
        placeholder = { 
            Text(
                "Select date",
                style = MaterialTheme.typography.bodySmall
            ) 
        },
        colors = OutlinedTextFieldDefaults.colors()
    )
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                ShadcnButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateChange(newDate)
                        }
                        showDatePicker = false
                    },
                    variant = ShadcnButtonVariant.Default,
                    size = ShadcnButtonSize.Default
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                ShadcnButton(
                    onClick = { showDatePicker = false },
                    variant = ShadcnButtonVariant.Outline,
                    size = ShadcnButtonSize.Default
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Export Dialog Component
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Expenses") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose export format:")
                Spacer(modifier = Modifier.height(8.dp))

                ExportFormat.values().forEach { format ->
                    TextButton(
                        onClick = { onExport(format) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(format.name)
                            Icon(Icons.Default.FileDownload, null)
                        }
                    }
                }
            }
        },
        confirmButton = {},
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