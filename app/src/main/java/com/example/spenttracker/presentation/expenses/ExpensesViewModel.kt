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
    
    init {
        // Start observing expenses from database
        observeExpenses()
    }
    
    /**
     * Observe expenses from repository and update UI state
     */
    private fun observeExpenses() {
        viewModelScope.launch {
            repository.getExpenses()
                .catch { e ->
                    _state.value = ExpenseListState.Error("Failed to load expenses: ${e.message}")
                }
                .collect { expenses ->
                    _state.value = ExpenseListState.Success(expenses)
                }
        }
    }
    
    /**
     * Add new expense
     */
    fun addExpense(description: String, amount: Double) {
        viewModelScope.launch {
            _isAddingExpense.value = true
            try {
                val expense = Expense(
                    description = description,
                    amount = amount,
                    date = LocalDate.now()
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
}