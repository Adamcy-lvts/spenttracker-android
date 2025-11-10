package com.example.spenttracker.presentation.budgets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.data.auth.SessionManager
import com.example.spenttracker.domain.model.*
import com.example.spenttracker.domain.repository.BudgetRepository
import com.example.spenttracker.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * UI State for Budget screen
 */
sealed class BudgetUiState {
    object Loading : BudgetUiState()
    data class Success(
        val summary: BudgetSummary,
        val selectedMonth: YearMonth = YearMonth.now()
    ) : BudgetUiState()
    data class Error(val message: String) : BudgetUiState()
}

/**
 * Events for Budget screen
 */
sealed class BudgetEvent {
    data class ShowSnackbar(val message: String) : BudgetEvent()
    data class BudgetCreated(val budget: Budget) : BudgetEvent()
    data class BudgetUpdated(val budget: Budget) : BudgetEvent()
    data class BudgetDeleted(val budgetId: Int) : BudgetEvent()
}

/**
 * Budget ViewModel
 * Manages budget state and operations
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "BudgetViewModel"
    }

    // Selected month for viewing budgets
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow<BudgetUiState>(BudgetUiState.Loading)
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    // Events
    private val _eventFlow = MutableSharedFlow<BudgetEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Loading states
    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _deletingBudgetId = MutableStateFlow<Int?>(null)
    val deletingBudgetId: StateFlow<Int?> = _deletingBudgetId.asStateFlow()

    // Categories for budget creation
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadCategories()
        loadBudgetSummary()
    }

    /**
     * Load all categories for budget creation
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getActiveCategories().collect { categoryList ->
                    _categories.value = categoryList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
            }
        }
    }

    /**
     * Load budget summary for selected month
     */
    private fun loadBudgetSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = BudgetUiState.Loading

                budgetRepository.getBudgetSummary(_selectedMonth.value).collect { summary ->
                    _uiState.value = BudgetUiState.Success(
                        summary = summary,
                        selectedMonth = _selectedMonth.value
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budget summary", e)
                _uiState.value = BudgetUiState.Error(e.message ?: "Failed to load budgets")
            }
        }
    }

    /**
     * Change selected month
     */
    fun selectMonth(month: YearMonth) {
        sessionManager.updateActivity()
        _selectedMonth.value = month
        loadBudgetSummary()
    }

    /**
     * Navigate to previous month
     */
    fun previousMonth() {
        selectMonth(_selectedMonth.value.minusMonths(1))
    }

    /**
     * Navigate to next month
     */
    fun nextMonth() {
        selectMonth(_selectedMonth.value.plusMonths(1))
    }

    /**
     * Reset to current month
     */
    fun resetToCurrentMonth() {
        selectMonth(YearMonth.now())
    }

    /**
     * Create a new budget
     */
    fun createBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                _isCreating.value = true
                sessionManager.updateActivity()

                val result = budgetRepository.createBudget(budget)

                result.onSuccess { createdBudget ->
                    Log.d(TAG, "Budget created successfully: ${createdBudget.id}")
                    _eventFlow.emit(BudgetEvent.BudgetCreated(createdBudget))
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Budget created successfully"))
                }.onFailure { error ->
                    Log.e(TAG, "Failed to create budget", error)
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Failed to create budget: ${error.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating budget", e)
                _eventFlow.emit(BudgetEvent.ShowSnackbar("Error creating budget: ${e.message}"))
            } finally {
                _isCreating.value = false
            }
        }
    }

    /**
     * Update an existing budget
     */
    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                _isUpdating.value = true
                sessionManager.updateActivity()

                val result = budgetRepository.updateBudget(budget)

                result.onSuccess { updatedBudget ->
                    Log.d(TAG, "Budget updated successfully: ${updatedBudget.id}")
                    _eventFlow.emit(BudgetEvent.BudgetUpdated(updatedBudget))
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Budget updated successfully"))
                }.onFailure { error ->
                    Log.e(TAG, "Failed to update budget", error)
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Failed to update budget: ${error.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating budget", e)
                _eventFlow.emit(BudgetEvent.ShowSnackbar("Error updating budget: ${e.message}"))
            } finally {
                _isUpdating.value = false
            }
        }
    }

    /**
     * Delete a budget
     */
    fun deleteBudget(budgetId: Int) {
        viewModelScope.launch {
            try {
                _deletingBudgetId.value = budgetId
                sessionManager.updateActivity()

                val result = budgetRepository.deleteBudget(budgetId)

                result.onSuccess {
                    Log.d(TAG, "Budget deleted successfully: $budgetId")
                    _eventFlow.emit(BudgetEvent.BudgetDeleted(budgetId))
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Budget deleted successfully"))
                }.onFailure { error ->
                    Log.e(TAG, "Failed to delete budget", error)
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Failed to delete budget: ${error.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting budget", e)
                _eventFlow.emit(BudgetEvent.ShowSnackbar("Error deleting budget: ${e.message}"))
            } finally {
                _deletingBudgetId.value = null
            }
        }
    }

    /**
     * Dismiss a budget alert
     */
    fun dismissAlert(alertId: Int) {
        viewModelScope.launch {
            try {
                sessionManager.updateActivity()

                val result = budgetRepository.dismissAlert(alertId)

                result.onSuccess {
                    Log.d(TAG, "Alert dismissed: $alertId")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to dismiss alert", error)
                    _eventFlow.emit(BudgetEvent.ShowSnackbar("Failed to dismiss alert: ${error.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing alert", e)
            }
        }
    }

    /**
     * Refresh budget data
     */
    fun refresh() {
        loadBudgetSummary()
    }
}
