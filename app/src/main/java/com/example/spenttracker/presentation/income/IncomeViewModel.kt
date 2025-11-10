package com.example.spenttracker.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.domain.model.Income
import com.example.spenttracker.domain.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * UI State for Income list
 */
sealed class IncomeListState {
    object Loading : IncomeListState()
    data class Success(val incomes: List<Income>) : IncomeListState()
    data class Error(val message: String) : IncomeListState()
}

/**
 * Events for Income operations
 */
sealed class IncomeEvent {
    data class ShowMessage(val message: String) : IncomeEvent()
    data class ShowError(val error: String) : IncomeEvent()
    object IncomeAdded : IncomeEvent()
    object IncomeUpdated : IncomeEvent()
    object IncomeDeleted : IncomeEvent()
}

/**
 * ViewModel for Income screen
 * Manages UI state and business logic for income management
 */
@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val repository: IncomeRepository
) : ViewModel() {

    // UI State
    private val _state = MutableStateFlow<IncomeListState>(IncomeListState.Loading)
    val state: StateFlow<IncomeListState> = _state.asStateFlow()

    // Events
    private val _eventFlow = MutableSharedFlow<IncomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Loading states
    private val _isAddingIncome = MutableStateFlow(false)
    val isAddingIncome: StateFlow<Boolean> = _isAddingIncome.asStateFlow()

    private val _deletingIncomeId = MutableStateFlow<Int?>(null)
    val deletingIncomeId: StateFlow<Int?> = _deletingIncomeId.asStateFlow()

    // All incomes for dashboard statistics
    private val _allIncomes = MutableStateFlow<List<Income>>(emptyList())
    val allIncomes: StateFlow<List<Income>> = _allIncomes.asStateFlow()

    // Month filter state
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    // Filtered incomes by month
    val filteredIncomes: StateFlow<List<Income>> = combine(
        _allIncomes,
        _selectedMonth
    ) { incomes, month ->
        incomes.filter { income ->
            YearMonth.from(income.date) == month
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Income sources for filtering
    private val _incomeSources = MutableStateFlow<List<String>>(emptyList())
    val incomeSources: StateFlow<List<String>> = _incomeSources.asStateFlow()

    init {
        loadIncomes()
        loadIncomeSources()
    }

    /**
     * Load all incomes from repository
     */
    fun loadIncomes() {
        viewModelScope.launch {
            _state.value = IncomeListState.Loading
            try {
                repository.getAllIncomes().collect { incomes ->
                    _allIncomes.value = incomes
                    _state.value = IncomeListState.Success(incomes)
                    android.util.Log.d("IncomeViewModel", "Loaded ${incomes.size} incomes")
                }
            } catch (e: Exception) {
                android.util.Log.e("IncomeViewModel", "Error loading incomes", e)
                _state.value = IncomeListState.Error(e.message ?: "Failed to load incomes")
            }
        }
    }

    /**
     * Load income sources for filter dropdown
     */
    private fun loadIncomeSources() {
        viewModelScope.launch {
            try {
                repository.getIncomeSources().collect { sources ->
                    _incomeSources.value = sources
                }
            } catch (e: Exception) {
                android.util.Log.e("IncomeViewModel", "Error loading income sources", e)
            }
        }
    }

    /**
     * Add a new income
     */
    fun addIncome(income: Income) {
        viewModelScope.launch {
            _isAddingIncome.value = true
            try {
                val result = repository.addIncome(income)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("IncomeViewModel", "Income added successfully")
                        _eventFlow.emit(IncomeEvent.IncomeAdded)
                        _eventFlow.emit(IncomeEvent.ShowMessage("Income added successfully"))
                    },
                    onFailure = { error ->
                        android.util.Log.e("IncomeViewModel", "Error adding income", error)
                        _eventFlow.emit(IncomeEvent.ShowError(error.message ?: "Failed to add income"))
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("IncomeViewModel", "Exception adding income", e)
                _eventFlow.emit(IncomeEvent.ShowError(e.message ?: "Failed to add income"))
            } finally {
                _isAddingIncome.value = false
            }
        }
    }

    /**
     * Update an existing income
     */
    fun updateIncome(income: Income) {
        viewModelScope.launch {
            _isAddingIncome.value = true
            try {
                val result = repository.updateIncome(income)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("IncomeViewModel", "Income updated successfully")
                        _eventFlow.emit(IncomeEvent.IncomeUpdated)
                        _eventFlow.emit(IncomeEvent.ShowMessage("Income updated successfully"))
                    },
                    onFailure = { error ->
                        android.util.Log.e("IncomeViewModel", "Error updating income", error)
                        _eventFlow.emit(IncomeEvent.ShowError(error.message ?: "Failed to update income"))
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("IncomeViewModel", "Exception updating income", e)
                _eventFlow.emit(IncomeEvent.ShowError(e.message ?: "Failed to update income"))
            } finally {
                _isAddingIncome.value = false
            }
        }
    }

    /**
     * Delete an income
     */
    fun deleteIncome(incomeId: Int) {
        viewModelScope.launch {
            _deletingIncomeId.value = incomeId
            try {
                val result = repository.deleteIncome(incomeId)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("IncomeViewModel", "Income deleted successfully")
                        _eventFlow.emit(IncomeEvent.IncomeDeleted)
                        _eventFlow.emit(IncomeEvent.ShowMessage("Income deleted successfully"))
                    },
                    onFailure = { error ->
                        android.util.Log.e("IncomeViewModel", "Error deleting income", error)
                        _eventFlow.emit(IncomeEvent.ShowError(error.message ?: "Failed to delete income"))
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("IncomeViewModel", "Exception deleting income", e)
                _eventFlow.emit(IncomeEvent.ShowError(e.message ?: "Failed to delete income"))
            } finally {
                _deletingIncomeId.value = null
            }
        }
    }

    /**
     * Set month filter
     */
    fun setSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    /**
     * Navigate to previous month
     */
    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    /**
     * Navigate to next month
     */
    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    /**
     * Reset to current month
     */
    fun resetToCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    /**
     * Get total income for current month
     */
    fun getTotalForMonth(month: YearMonth): Double {
        return allIncomes.value
            .filter { YearMonth.from(it.date) == month }
            .sumOf { it.amount }
    }

    /**
     * Get recurring incomes
     */
    fun getRecurringIncomes(): List<Income> {
        return allIncomes.value.filter { it.isRecurring }
    }
}
