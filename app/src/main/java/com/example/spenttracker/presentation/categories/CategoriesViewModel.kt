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
    
    // Operation states
    private val _isAddingCategory = MutableStateFlow(false)
    val isAddingCategory: StateFlow<Boolean> = _isAddingCategory.asStateFlow()
    
    private val _isUpdatingCategory = MutableStateFlow(false)
    val isUpdatingCategory: StateFlow<Boolean> = _isUpdatingCategory.asStateFlow()
    
    private val _isDeletingCategory = MutableStateFlow(false)
    val isDeletingCategory: StateFlow<Boolean> = _isDeletingCategory.asStateFlow()
    
    // Events (like Vue.js toast notifications)
    private val _eventFlow = MutableSharedFlow<CategoryListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    
    init {
        loadCategories()
        createDefaultCategoriesIfEmpty()
    }
    
    /**
     * Create default categories if none exist (first app launch)
     */
    private fun createDefaultCategoriesIfEmpty() {
        viewModelScope.launch {
            try {
                val existingCategories = repository.getAllCategories()
                if (existingCategories.isEmpty()) {
                    // Default categories matching typical expense categories
                    val defaultCategories = listOf(
                        Category(name = "Food & Dining", color = "#F59E0B", description = "Restaurants, groceries, and food delivery"),
                        Category(name = "Transportation", color = "#10B981", description = "Gas, parking, public transit, ride-sharing"),
                        Category(name = "Shopping", color = "#EC4899", description = "Clothing, electronics, and general purchases"),
                        Category(name = "Bills & Utilities", color = "#EF4444", description = "Rent, electricity, phone, internet"),
                        Category(name = "Entertainment", color = "#8B5CF6", description = "Movies, games, subscriptions, hobbies"),
                        Category(name = "Health & Medical", color = "#06B6D4", description = "Doctor visits, pharmacy, medical expenses"),
                        Category(name = "Education", color = "#3B82F6", description = "Books, courses, school fees"),
                        Category(name = "Travel", color = "#84CC16", description = "Hotels, flights, vacation expenses"),
                        Category(name = "Personal Care", color = "#F97316", description = "Haircuts, cosmetics, personal items"),
                        Category(name = "Other", color = "#6B7280", description = "Miscellaneous expenses")
                    )
                    
                    defaultCategories.forEach { category ->
                        repository.addCategory(category)
                    }
                }
            } catch (e: Exception) {
                // Fail silently for default categories - not critical
            }
        }
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
    
    /**
     * Add new category
     */
    fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                _isAddingCategory.value = true
                repository.addCategory(category)
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Category added successfully! 🎉"))
            } catch (e: Exception) {
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Failed to add category: ${e.message}"))
            } finally {
                _isAddingCategory.value = false
            }
        }
    }
    
    /**
     * Update existing category
     */
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                _isUpdatingCategory.value = true
                repository.updateCategory(category)
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Category updated successfully! ✏️"))
            } catch (e: Exception) {
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Failed to update category: ${e.message}"))
            } finally {
                _isUpdatingCategory.value = false
            }
        }
    }
    
    /**
     * Delete category
     */
    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                _isDeletingCategory.value = true
                repository.deleteCategory(categoryId)
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Category deleted successfully! 🗑️"))
            } catch (e: Exception) {
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Failed to delete category: ${e.message}"))
            } finally {
                _isDeletingCategory.value = false
            }
        }
    }
    
    /**
     * Toggle category active status
     */
    fun toggleCategoryStatus(categoryId: Int) {
        viewModelScope.launch {
            try {
                repository.toggleCategoryStatus(categoryId)
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Category status updated! 🔄"))
            } catch (e: Exception) {
                _eventFlow.emit(CategoryListEvent.ShowSnackbar("Failed to update status: ${e.message}"))
            }
        }
    }
    
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
 * Like Vue.js event system (toast notifications, navigation)
 */
sealed class CategoryListEvent {
    data class ShowSnackbar(val message: String) : CategoryListEvent()
    data class NavigateToEdit(val categoryId: Int) : CategoryListEvent()
}