package com.example.spenttracker.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.data.auth.UserContextProviderImpl
import com.example.spenttracker.data.auth.SessionManager
import com.example.spenttracker.data.sync.SyncScheduler
import com.example.spenttracker.domain.model.Expense
import com.example.spenttracker.domain.repository.ExpenseRepository
import com.example.spenttracker.util.ExpenseExportManager
import com.example.spenttracker.util.ExportFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Date filter types for expense filtering
 */
enum class DateFilterType {
    ALL,
    TODAY,
    THIS_MONTH,
    CUSTOM_DATE,
    CUSTOM_MONTH
}

/**
 * ViewModel for Expenses screen - Like Laravel's Controller
 * Manages UI state and business logic for the expenses feature
 * 
 * Kotlin Syntax Explanations:
 * - @HiltViewModel: Like Laravel's dependency injection in controller constructor
 * - @Inject constructor: Tells Hilt to inject dependencies automatically
 * - private val: Immutable property (like final in Java, const in PHP)
 * - : ViewModel(): Kotlin inheritance syntax (like extends in Java)
 */
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository, // Like Laravel: ExpenseService $expenseService
    private val userContextProvider: UserContextProviderImpl,
    private val sessionManager: SessionManager,
    private val syncScheduler: SyncScheduler,
    private val exportManager: ExpenseExportManager
) : ViewModel() {
    
    /*
     * KOTLIN STATE MANAGEMENT (Like Laravel's session/cache variables)
     * 
     * Kotlin Syntax Explanations:
     * - MutableStateFlow<T>: Like Laravel's session variable that can change
     * - StateFlow<T>: Like Laravel's read-only session variable 
     * - private val _state: Private mutable version (like private $state in Laravel)
     * - val state: Public read-only version (like public getter in Laravel)
     * - <ExpenseListState>: Generic type (like Laravel's typed properties)
     * - asStateFlow(): Converts mutable to read-only (like Laravel's readonly accessor)
     */
    
    // UI State - Like Laravel's $expenses variable in controller
    private val _state = MutableStateFlow<ExpenseListState>(ExpenseListState.Loading)
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()
    
    // Events - Like Laravel's flash messages or event broadcasting
    private val _eventFlow = MutableSharedFlow<ExpenseListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    // Loading state for form submission - Like Laravel's $isLoading flag
    private val _isAddingExpense = MutableStateFlow(false)
    val isAddingExpense: StateFlow<Boolean> = _isAddingExpense.asStateFlow()
    
    // Loading state for delete operations - Like Laravel's $deletingId
    private val _deletingExpenseId = MutableStateFlow<Int?>(null) // Int? means nullable Int
    val deletingExpenseId: StateFlow<Int?> = _deletingExpenseId.asStateFlow()
    
    // Pagination state
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _pageSize = MutableStateFlow(10)
    val pageSize: StateFlow<Int> = _pageSize.asStateFlow()
    
    private val _totalExpenses = MutableStateFlow(0)
    val totalExpenses: StateFlow<Int> = _totalExpenses.asStateFlow()
    
    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _allExpenses.asStateFlow()
    
    // Date filter state (must be declared before filteredExpenses that uses them)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    private val _dateFilterType = MutableStateFlow(DateFilterType.THIS_MONTH) // Default to this month
    val dateFilterType: StateFlow<DateFilterType> = _dateFilterType.asStateFlow()
    
    // Computed property for filtered expenses
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        _allExpenses,
        _dateFilterType,
        _selectedDate
    ) { expenses, filterType, selectedDate ->
        when (filterType) {
            DateFilterType.ALL -> expenses
            DateFilterType.TODAY -> expenses.filter { it.date == LocalDate.now() }
            DateFilterType.THIS_MONTH -> {
                val currentMonth = YearMonth.now()
                expenses.filter { YearMonth.from(it.date) == currentMonth }
            }
            DateFilterType.CUSTOM_DATE -> expenses.filter { it.date == selectedDate }
            DateFilterType.CUSTOM_MONTH -> {
                val selectedMonth = YearMonth.from(selectedDate)
                expenses.filter { YearMonth.from(it.date) == selectedMonth }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )
    
    // Keep for backwards compatibility
    val showAllDates: StateFlow<Boolean> = _dateFilterType.map { it == DateFilterType.ALL }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )
    
    init {
        // Start observing expenses from database when user context changes
        observeExpenses()
    }
    
    /**
     * Observe expenses from repository and update UI state with pagination
     * Restarts when user context changes to ensure proper data isolation
     */
    private fun observeExpenses() {
        viewModelScope.launch {
            userContextProvider.currentUserIdFlow
                .flatMapLatest { userId ->
                    android.util.Log.d("ExpensesViewModel", "User context changed to: $userId")
                    if (userId != null) {
                        repository.getExpenses()
                    } else {
                        flowOf(emptyList())
                    }
                }
                .catch { e ->
                    android.util.Log.e("ExpensesViewModel", "Error loading expenses: ${e.message}")
                    _state.value = ExpenseListState.Error("Failed to load expenses: ${e.message}")
                }
                .collect { expenses ->
                    android.util.Log.d("ExpensesViewModel", "Received ${expenses.size} expenses")
                    _allExpenses.value = expenses.sortedByDescending { it.date }
                    _totalExpenses.value = expenses.size
                    updatePaginatedExpenses()
                }
        }
    }
    
    /**
     * Update the displayed expenses based on current page and date filter
     */
    private fun updatePaginatedExpenses() {
        val allExpenses = _allExpenses.value
        val filterType = _dateFilterType.value
        val selectedDate = _selectedDate.value
        
        // Apply date filter based on filter type
        val filteredExpenses = when (filterType) {
            DateFilterType.ALL -> allExpenses
            DateFilterType.TODAY -> allExpenses.filter { it.date == LocalDate.now() }
            DateFilterType.THIS_MONTH -> {
                val currentMonth = YearMonth.now()
                allExpenses.filter { YearMonth.from(it.date) == currentMonth }
            }
            DateFilterType.CUSTOM_DATE -> allExpenses.filter { it.date == selectedDate }
            DateFilterType.CUSTOM_MONTH -> {
                val selectedMonth = YearMonth.from(selectedDate)
                allExpenses.filter { YearMonth.from(it.date) == selectedMonth }
            }
        }
        
        // Update total count for filtered results
        _totalExpenses.value = filteredExpenses.size
        
        // Apply pagination
        val page = _currentPage.value
        val size = _pageSize.value
        
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, filteredExpenses.size)
        
        val paginatedExpenses = if (startIndex < filteredExpenses.size) {
            filteredExpenses.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        _state.value = ExpenseListState.Success(paginatedExpenses)
    }
    
    /**
     * Add new expense
     */
    fun addExpense(description: String, amount: Double, categoryId: Int? = null, date: LocalDate = LocalDate.now()) {
        // Track user activity for session management
        sessionManager.updateActivity()
        
        viewModelScope.launch {
            _isAddingExpense.value = true
            try {
                val expense = Expense(
                    description = description,
                    amount = amount,
                    date = date,
                    categoryId = categoryId
                )
                repository.addExpense(expense)
                
                // Trigger upload sync after creating expense
                syncScheduler.triggerUploadSync()
                
                _eventFlow.emit(ExpenseListEvent.ShowSnackbar("Expense added successfully"))
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseListEvent.ShowSnackbar("Failed to add expense: ${e.message}"))
            } finally {
                _isAddingExpense.value = false
            }
        }
    }
    
    /**
     * Delete expense
     */
    fun deleteExpense(id: Int) {
        // Track user activity for session management
        sessionManager.updateActivity()
        
        viewModelScope.launch {
            _deletingExpenseId.value = id
            try {
                repository.deleteExpense(id)
                
                // Trigger upload sync after deleting expense
                syncScheduler.triggerUploadSync()
                
                _eventFlow.emit(ExpenseListEvent.ShowSnackbar("Expense deleted"))
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseListEvent.ShowSnackbar("Failed to delete expense: ${e.message}"))
            } finally {
                _deletingExpenseId.value = null
            }
        }
    }
    
    /**
     * Update existing expense
     */
    fun updateExpense(expense: Expense) {
        // Track user activity for session management
        sessionManager.updateActivity()
        
        viewModelScope.launch {
            try {
                repository.updateExpense(expense)
                
                // Trigger upload sync after updating expense
                syncScheduler.triggerUploadSync()
                
                _eventFlow.emit(ExpenseListEvent.ShowSnackbar("Expense updated successfully"))
            } catch (e: Exception) {
                _eventFlow.emit(ExpenseListEvent.ShowSnackbar("Failed to update expense: ${e.message}"))
            }
        }
    }
    
    /**
     * Refresh expenses
     */
    fun refreshExpenses() {
        // For local-only repository, just trigger re-observation
        observeExpenses()
    }
    
    /**
     * Navigate to next page
     */
    fun nextPage() {
        // Track user activity for session management
        sessionManager.updateActivity()
        
        val totalPages = getTotalPages()
        val currentPage = _currentPage.value
        if (currentPage < totalPages - 1) {
            _currentPage.value = currentPage + 1
            updatePaginatedExpenses()
        }
    }
    
    /**
     * Navigate to previous page
     */
    fun previousPage() {
        // Track user activity for session management
        sessionManager.updateActivity()
        
        val currentPage = _currentPage.value
        if (currentPage > 0) {
            _currentPage.value = currentPage - 1
            updatePaginatedExpenses()
        }
    }
    
    /**
     * Navigate to specific page
     */
    fun goToPage(page: Int) {
        val totalPages = getTotalPages()
        if (page in 0 until totalPages) {
            _currentPage.value = page
            updatePaginatedExpenses()
        }
    }
    
    /**
     * Get total number of pages
     */
    fun getTotalPages(): Int {
        val total = _totalExpenses.value
        val size = _pageSize.value
        return if (total == 0) 1 else (total + size - 1) / size
    }
    
    /**
     * Check if there's a next page
     */
    fun hasNextPage(): Boolean = _currentPage.value < getTotalPages() - 1
    
    /**
     * Check if there's a previous page
     */
    fun hasPreviousPage(): Boolean = _currentPage.value > 0
    
    /**
     * Update page size and reset to first page
     */
    fun updatePageSize(newSize: Int) {
        _pageSize.value = newSize
        _currentPage.value = 0 // Reset to first page
        updatePaginatedExpenses()
    }
    
    /**
     * Set date filter to specific date
     */
    fun filterByDate(date: LocalDate) {
        // Track user activity for session management
        sessionManager.updateActivity()
        
        _selectedDate.value = date
        _dateFilterType.value = DateFilterType.CUSTOM_DATE
        _currentPage.value = 0 // Reset to first page
        updatePaginatedExpenses()
    }
    
    /**
     * Set filter to show all expenses
     */
    fun showAllExpenses() {
        sessionManager.updateActivity()
        _dateFilterType.value = DateFilterType.ALL
        _currentPage.value = 0
        updatePaginatedExpenses()
    }
    
    /**
     * Set filter to show today's expenses
     */
    fun showTodayExpenses() {
        sessionManager.updateActivity()
        _dateFilterType.value = DateFilterType.TODAY
        _currentPage.value = 0
        updatePaginatedExpenses()
    }
    
    /**
     * Set filter to show this month's expenses (default)
     */
    fun showThisMonthExpenses() {
        sessionManager.updateActivity()
        _dateFilterType.value = DateFilterType.THIS_MONTH
        _currentPage.value = 0
        updatePaginatedExpenses()
    }

    /**
     * Filter expenses by specific month
     */
    fun filterByMonth(month: YearMonth) {
        sessionManager.updateActivity()
        // Set filter to custom month and update the selected date to the first day of the month
        _selectedDate.value = month.atDay(1)
        _dateFilterType.value = DateFilterType.CUSTOM_MONTH
        _currentPage.value = 0
        updatePaginatedExpenses()
    }

    /**
     * Set filter to today's date
     */
    fun filterByToday() {
        showTodayExpenses()
    }
    
    /**
     * Get the current filtered expenses for calculating totals
     */
    fun getCurrentFilteredExpenses(): List<Expense> {
        val allExpenses = _allExpenses.value
        val filterType = _dateFilterType.value
        val selectedDate = _selectedDate.value

        return when (filterType) {
            DateFilterType.ALL -> allExpenses
            DateFilterType.TODAY -> allExpenses.filter { it.date == LocalDate.now() }
            DateFilterType.THIS_MONTH -> {
                val currentMonth = YearMonth.now()
                allExpenses.filter { YearMonth.from(it.date) == currentMonth }
            }
            DateFilterType.CUSTOM_DATE -> allExpenses.filter { it.date == selectedDate }
            DateFilterType.CUSTOM_MONTH -> {
                val selectedMonth = YearMonth.from(selectedDate)
                allExpenses.filter { YearMonth.from(it.date) == selectedMonth }
            }
        }
    }

    /**
     * Export expenses to file
     */
    fun exportExpenses(format: ExportFormat, fileName: String? = null): Flow<Result<File>> = flow {
        try {
            val expenses = getCurrentFilteredExpenses()
            if (expenses.isEmpty()) {
                emit(Result.failure(Exception("No expenses to export")))
                return@flow
            }

            val result = exportManager.exportExpenses(expenses, format, fileName)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
