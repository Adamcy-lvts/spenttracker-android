package com.example.spenttracker.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.domain.repository.CategoryRepository
import com.example.spenttracker.data.local.CategoryDao
import com.example.spenttracker.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CategoryViewModel for managing categories in expense forms
 * Uses Hilt dependency injection like Laravel's service container
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val categoryDao: CategoryDao,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        initializeCategories()
        loadCategories()
    }

    /**
     * Categories are now global - no initialization needed
     * Categories will be synced from server
     */
    private fun initializeCategories() {
        // Categories are managed globally - no local initialization needed
        // They will be loaded from server via sync
    }

    /**
     * Load all active categories
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            categoryRepository.getActiveCategories().collect { categoryList ->
                _categories.value = categoryList
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh categories from database
     */
    fun refreshCategories() {
        loadCategories()
    }

    // Categories are now read-only for users (managed globally by admin)
    // Removed: addCategory, updateCategory, deleteCategory methods
}

// CategoryOperationResult removed - no longer needed for read-only categories