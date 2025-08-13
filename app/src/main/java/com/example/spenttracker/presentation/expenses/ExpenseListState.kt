package com.example.spenttracker.presentation.expenses

import com.example.spenttracker.domain.model.Expense

/**
 * UI state for expense list screen
 * Represents different states the UI can be in
 */
sealed class ExpenseListState {
    object Loading : ExpenseListState()
    data class Success(val expenses: List<Expense>) : ExpenseListState()
    data class Error(val message: String) : ExpenseListState()
}

/**
 * UI events that can be triggered from the screen
 */
sealed class ExpenseListEvent {
    data class ShowSnackbar(val message: String) : ExpenseListEvent()
    data class NavigateToEdit(val expenseId: Int) : ExpenseListEvent()
}