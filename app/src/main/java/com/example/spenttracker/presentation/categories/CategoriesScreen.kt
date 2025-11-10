package com.example.spenttracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// Removed Add, Delete, Edit icons - Categories are now read-only
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.data.local.ExpenseDatabase
import com.example.spenttracker.presentation.auth.AuthViewModel
import com.example.spenttracker.data.repository.CategoryRepositoryImpl
import com.example.spenttracker.data.mapper.toDomainWithCountList
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.presentation.theme.ShadcnButton
import com.example.spenttracker.presentation.theme.ShadcnButtonVariant
import com.example.spenttracker.presentation.theme.ShadcnButtonSize

/**
 * Categories Screen - Matches the Vue.js design with active/inactive sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    darkTheme: Boolean = false,
    onDarkThemeToggle: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    // Get current user from auth
    val authViewModel: AuthViewModel = hiltViewModel()
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUserId = authUiState.currentUser?.id ?: 0L
    
    // Create ViewModel with repository
    val context = LocalContext.current
    val database = ExpenseDatabase.getDatabase(context)
    val repository = CategoryRepositoryImpl(database.categoryDao())
    val viewModel: CategoriesViewModel = viewModel { CategoriesViewModel(repository) }
    
    // Collect UI state from ViewModel
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Get user-specific categories with correct expense counts
    val userSpecificCategoriesFlow = remember(currentUserId) {
        database.categoryDao().getCategoriesWithExpenseCountForUser(currentUserId)
    }
    val userSpecificCategoriesData by userSpecificCategoriesFlow.collectAsState(initial = emptyList())
    
    // Convert to domain models
    val userSpecificCategories = remember(userSpecificCategoriesData) {
        userSpecificCategoriesData.toDomainWithCountList()
    }
    
    // Categories are now read-only - no dialogs needed
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Categories",
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
                }
            )
        }
        // Removed FloatingActionButton - Categories are read-only
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Description
            item {
                Text(
                    text = "Categories are managed globally for consistent analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            when (val currentState = state) {
                is CategoryListState.Loading -> {
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
                
                is CategoryListState.Success -> {
                    // Use user-specific categories with correct expense counts
                    val activeCategories = userSpecificCategories.filter { it.isActive }
                    val inactiveCategories = userSpecificCategories.filter { !it.isActive }
                    
                    // Active Categories Section
                    item {
                        CategoriesSection(
                            title = "Active Categories",
                            subtitle = "${activeCategories.size} categories",
                            categories = activeCategories,
                            emptyMessage = "No active categories",
                            emptySubMessage = "Categories will be loaded from the server"
                        )
                    }
                    
                    // Inactive Categories Section (if any)
                    if (inactiveCategories.isNotEmpty()) {
                        item {
                            CategoriesSection(
                                title = "Inactive Categories",
                                subtitle = "${inactiveCategories.size} categories",
                                categories = inactiveCategories,
                                isInactive = true
                            )
                        }
                    }
                }
                
                is CategoryListState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Categories are now read-only - no dialogs needed
}

/**
 * Categories Section Component
 * Displays a section of categories (active or inactive)
 */
@Composable
fun CategoriesSection(
    title: String,
    subtitle: String,
    categories: List<Category>,
    emptyMessage: String = "No categories",
    emptySubMessage: String = "",
    isInactive: Boolean = false
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (categories.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (emptySubMessage.isNotEmpty()) {
                        Text(
                            text = emptySubMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Categories list (single column)
                categories.forEach { category ->
                    CategoryCard(
                        category = category,
                        isInactive = isInactive,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Category Card Component
 * Individual category display card
 */
@Composable
fun CategoryCard(
    category: Category,
    isInactive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(
                if (isInactive) Modifier else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInactive) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = if (isInactive) 0.5f else 1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color indicator and category name in the same row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isInactive) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            // Description (if available) - full width and multi-line
            if (!category.description.isNullOrBlank()) {
                Text(
                    text = category.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isInactive) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// CategoryActionsDropdown removed - Categories are now read-only