package com.example.spenttracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spenttracker.data.local.ExpenseDatabase
import com.example.spenttracker.data.repository.CategoryRepositoryImpl
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
    // Create ViewModel with repository
    val context = LocalContext.current
    val database = ExpenseDatabase.getDatabase(context)
    val repository = CategoryRepositoryImpl(database.categoryDao())
    val viewModel: CategoriesViewModel = viewModel { CategoriesViewModel(repository) }
    
    // Collect UI state from ViewModel
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
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
                },
                actions = {
                    // Dark mode toggle switch
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { onDarkThemeToggle() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(32.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Category"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    ShadcnButton(
                        onClick = { showAddDialog = true },
                        variant = ShadcnButtonVariant.Default,
                        size = ShadcnButtonSize.Default
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Category")
                    }
                }
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
                    val activeCategories = currentState.categories.filter { it.isActive }
                    val inactiveCategories = currentState.categories.filter { !it.isActive }
                    
                    // Active Categories Section
                    item {
                        CategoriesSection(
                            title = "Active Categories",
                            subtitle = "${activeCategories.size} categories",
                            categories = activeCategories,
                            onEditClick = { category ->
                                selectedCategory = category
                                showEditDialog = true
                            },
                            onDeleteClick = { category ->
                                selectedCategory = category
                                showDeleteDialog = true
                            },
                            emptyMessage = "No active categories",
                            emptySubMessage = "Create your first category to organize expenses"
                        )
                    }
                    
                    // Inactive Categories Section (if any)
                    if (inactiveCategories.isNotEmpty()) {
                        item {
                            CategoriesSection(
                                title = "Inactive Categories",
                                subtitle = "${inactiveCategories.size} categories",
                                categories = inactiveCategories,
                                onEditClick = { category ->
                                    selectedCategory = category
                                    showEditDialog = true
                                },
                                onDeleteClick = { category ->
                                    selectedCategory = category
                                    showDeleteDialog = true
                                },
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
    
    // Add Category Dialog
    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { category ->
                viewModel.addCategory(category)
                showAddDialog = false
            }
        )
    }
    
    // Edit Category Dialog
    if (showEditDialog && selectedCategory != null) {
        EditCategoryDialog(
            category = selectedCategory!!,
            onDismiss = { 
                showEditDialog = false
                selectedCategory = null
            },
            onSave = { category ->
                viewModel.updateCategory(category)
                showEditDialog = false
                selectedCategory = null
            }
        )
    }
    
    // Delete Category Dialog
    if (showDeleteDialog && selectedCategory != null) {
        DeleteCategoryDialog(
            category = selectedCategory!!,
            onDismiss = { 
                showDeleteDialog = false
                selectedCategory = null
            },
            onConfirm = {
                viewModel.deleteCategory(selectedCategory!!.id)
                showDeleteDialog = false
                selectedCategory = null
            }
        )
    }
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
    onEditClick: (Category) -> Unit,
    onDeleteClick: (Category) -> Unit,
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
                // Categories grid
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { category ->
                            CategoryCard(
                                category = category,
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                isInactive = isInactive,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
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
    onEditClick: (Category) -> Unit,
    onDeleteClick: (Category) -> Unit,
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
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with color dot and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color indicator
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isInactive) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                
                // Actions dropdown
                CategoryActionsDropdown(
                    onEditClick = { onEditClick(category) },
                    onDeleteClick = { onDeleteClick(category) }
                )
            }
            
            // Description (if available)
            category.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInactive) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Expense count
            Text(
                text = "${category.expenseCount} ${if (category.expenseCount == 1) "expense" else "expenses"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isInactive) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Category Actions Dropdown Component
 * 3-dots menu for category actions
 */
@Composable
fun CategoryActionsDropdown(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        // 3 vertical dots button
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        
        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Edit option
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onClick = {
                    onEditClick()
                    expanded = false
                }
            )
            
            // Delete option
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                onClick = {
                    onDeleteClick()
                    expanded = false
                }
            )
        }
    }
}