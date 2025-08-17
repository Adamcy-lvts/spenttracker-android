package com.example.spenttracker.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spenttracker.domain.model.Category
import com.example.spenttracker.presentation.theme.ShadcnButton
import com.example.spenttracker.presentation.theme.ShadcnTextField
import com.example.spenttracker.presentation.theme.ShadcnButtonVariant
import com.example.spenttracker.presentation.theme.ShadcnButtonSize

/**
 * Add Category Dialog
 * Matches the Vue.js dialog structure with color picker
 */
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#3B82F6") } // Default blue
    var description by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    
    // Predefined colors matching Vue.js app
    val colorOptions = listOf(
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
        "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6B7280"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Add New Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create a new category to organize your expenses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Name field
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ShadcnTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g., Food & Dining") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Color picker
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current color preview
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor(color)),
                                    CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                        
                        Text(
                            text = color,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Color options grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.chunked(5).forEach { rowColors ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowColors.forEach { colorOption ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(colorOption)),
                                                CircleShape
                                            )
                                            .border(
                                                width = if (color == colorOption) 2.dp else 1.dp,
                                                color = if (color == colorOption) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable { color = colorOption }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Description field
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Description (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ShadcnTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Brief description of this category...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                
                // Active toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            ShadcnButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val category = Category(
                            name = name.trim(),
                            color = color,
                            description = description.takeIf { it.isNotBlank() },
                            isActive = isActive
                        )
                        onSave(category)
                    }
                },
                enabled = name.isNotBlank(),
                variant = ShadcnButtonVariant.Default,
                size = ShadcnButtonSize.Default
            ) {
                Text("Create Category")
            }
        },
        dismissButton = {
            ShadcnButton(
                onClick = onDismiss,
                variant = ShadcnButtonVariant.Outline,
                size = ShadcnButtonSize.Default
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Edit Category Dialog
 * Pre-populated with existing category data
 */
@Composable
fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var color by remember { mutableStateOf(category.color) }
    var description by remember { mutableStateOf(category.description ?: "") }
    var isActive by remember { mutableStateOf(category.isActive) }
    
    // Predefined colors matching Vue.js app
    val colorOptions = listOf(
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
        "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6B7280"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Edit Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Update category details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Name field
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ShadcnTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Color picker
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current color preview
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor(color)),
                                    CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                        
                        Text(
                            text = color,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Color options grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.chunked(5).forEach { rowColors ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowColors.forEach { colorOption ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(colorOption)),
                                                CircleShape
                                            )
                                            .border(
                                                width = if (color == colorOption) 2.dp else 1.dp,
                                                color = if (color == colorOption) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable { color = colorOption }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Description field
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Description (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ShadcnTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                
                // Active toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            ShadcnButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val updatedCategory = category.copy(
                            name = name.trim(),
                            color = color,
                            description = description.takeIf { it.isNotBlank() },
                            isActive = isActive
                        )
                        onSave(updatedCategory)
                    }
                },
                enabled = name.isNotBlank(),
                variant = ShadcnButtonVariant.Default,
                size = ShadcnButtonSize.Default
            ) {
                Text("Update Category")
            }
        },
        dismissButton = {
            ShadcnButton(
                onClick = onDismiss,
                variant = ShadcnButtonVariant.Outline,
                size = ShadcnButtonSize.Default
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Delete Category Dialog
 * Confirmation dialog for category deletion
 */
@Composable
fun DeleteCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Delete Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete \"${category.name}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (category.expenseCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚠️ Warning",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "This category has ${category.expenseCount} associated ${if (category.expenseCount == 1) "expense" else "expenses"}. They will lose their category assignment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ShadcnButton(
                onClick = onConfirm,
                variant = ShadcnButtonVariant.Destructive,
                size = ShadcnButtonSize.Default
            ) {
                Text("Delete Category")
            }
        },
        dismissButton = {
            ShadcnButton(
                onClick = onDismiss,
                variant = ShadcnButtonVariant.Outline,
                size = ShadcnButtonSize.Default
            ) {
                Text("Cancel")
            }
        }
    )
}