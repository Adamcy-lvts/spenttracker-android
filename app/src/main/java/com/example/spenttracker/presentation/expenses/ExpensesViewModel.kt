package com.example.spenttracker.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.domain.model.Expense
import com.example.spenttracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for Expenses screen
 * Manages UI state and business logic
 */
class ExpensesViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {
    
    // UI State
    private val _state = MutableStateFlow<ExpenseListState>(ExpenseListState.Loading)
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()
    
    // Events
    private val _eventFlow = MutableSharedFlow<ExpenseListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    // Loading state for form submission
    private val _isAddingExpense = MutableStateFlow(false)
    val isAddingExpense: StateFlow<Boolean> = _isAddingExpense.asStateFlow()
    
    // Loading state for delete operations
    private val _deletingExpenseId = MutableStateFlow<Int?>(null)
    val deletingExpenseId: StateFlow<Int?> = _deletingExpenseId.asStateFlow()
    
    // Pagination state
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _pageSize = MutableStateFlow(5)
    val pageSize: StateFlow<Int> = _pageSize.asStateFlow()
    
    private val _totalExpenses = MutableStateFlow(0)
    val totalExpenses: StateFlow<Int> = _totalExpenses.asStateFlow()
    
    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    
    // Date filter state
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    private val _showAllDates = MutableStateFlow(true)
    val showAllDates: StateFlow<Boolean> = _showAllDates.asStateFlow()
    
    init {
        // Start observing expenses from database
        observeExpenses()
    }
    
    /**
     * Observe expenses from repository and update UI state with pagination
     */
    private fun observeExpenses() {
        viewModelScope.launch {
            repository.getExpenses()
                .catch { e ->
                    _state.value = ExpenseListState.Error("Failed to load expenses: ${e.message}")
                }
                .collect { expenses ->
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
        val showAll = _showAllDates.value
        val selectedDate = _selectedDate.value
        
        // Apply date filter
        val filteredExpenses = if (showAll) {
            allExpenses
        } else {
            allExpenses.filter { it.date == selectedDate }
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
    fun addExpense(description: String, amount: Double, categoryId: Int? = null) {
        viewModelScope.launch {
            _isAddingExpense.value = true
            try {
                val expense = Expense(
                    description = description,
                    amount = amount,
                    date = LocalDate.now(),
                    categoryId = categoryId
                )
                repository.addExpense(expense)
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
        viewModelScope.launch {
            _deletingExpenseId.value = id
            try {
                repository.deleteExpense(id)
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
        viewModelScope.launch {
            try {
                repository.updateExpense(expense)
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
        _selectedDate.value = date
        _showAllDates.value = false
        _currentPage.value = 0 // Reset to first page
        updatePaginatedExpenses()
    }
    
    /**
     * Show all dates (remove filter)
     */
    fun showAllDates() {
        _showAllDates.value = true
        _currentPage.value = 0 // Reset to first page
        updatePaginatedExpenses()
    }
    
    /**
     * Set filter to today's date
     */
    fun filterByToday() {
        filterByDate(LocalDate.now())
    }
}