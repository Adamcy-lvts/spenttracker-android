package com.example.spenttracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Categories ViewModel
 * Manages category state and operations like the Vue.js composable pattern
 */
class CategoriesViewModel(
    private val repository: CategoryRepository
) : ViewModel() {
    
    // UI State
    private val _state = MutableStateFlow<CategoryListState>(CategoryListState.Loading)
    val state: StateFlow<CategoryListState> = _state.asStateFlow()
    
    // Loading state for operations
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Categories are now read-only - no operation states needed
    
    // Events (like Vue.js toast notifications) - simplified
    private val _eventFlow = MutableSharedFlow<CategoryListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    init {
        loadCategories()
        createDefaultCategoriesIfEmpty()
    }
    
    /**
     * Categories are now global - no local initialization needed
     * Categories will be synced from server
     */
    private fun createDefaultCategoriesIfEmpty() {
        // Categories are managed globally - no local initialization needed
        // They will be loaded from server via sync
    }
    
    /**
     * Load categories with expense counts
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getCategoriesWithExpenseCount().collect { categories ->
                    _state.value = CategoryListState.Success(categories)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _state.value = CategoryListState.Error(e.message ?: "Unknown error")
                _isLoading.value = false
            }
        }
    }
    
    // Categories are now read-only for users (managed globally by admin)
    // Removed: addCategory, updateCategory, deleteCategory, toggleCategoryStatus methods
    
    /**
     * Refresh categories
     */
    fun refreshCategories() {
        loadCategories()
    }
}

/**
 * Category List UI State
 * Like Vue.js reactive data patterns
 */
sealed class CategoryListState {
    object Loading : CategoryListState()
    data class Success(val categories: List<Category>) : CategoryListState()
    data class Error(val message: String) : CategoryListState()
}

/**
 * Category List Events
 * Like Vue.js event system (simplified for read-only categories)
 */
sealed class CategoryListEvent {
    data class ShowSnackbar(val message: String) : CategoryListEvent()
}