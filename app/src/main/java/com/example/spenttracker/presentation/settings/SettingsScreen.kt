package com.example.spenttracker.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.spenttracker.domain.model.ReminderSettings
import com.example.spenttracker.domain.model.SyncFrequency
import com.example.spenttracker.domain.model.getPresetReminderTimes
import com.example.spenttracker.domain.model.getPresetReminderMessages
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Settings Screen - Comprehensive settings management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkTheme: Boolean = false,
    onDarkThemeToggle: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    
    // Dialog states
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showEditReminderDialog by remember { mutableStateOf(false) }
    var showDeleteReminderDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<ReminderSettings?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications Section
            item {
                SettingsSection(title = "Notifications") {
                    // Global notifications toggle
                    SettingsToggleItem(
                        title = "Enable Notifications",
                        subtitle = "Receive expense reminder notifications",
                        icon = Icons.Default.Notifications,
                        isChecked = uiState.appSettings.notificationsEnabled,
                        onToggle = settingsViewModel::toggleNotifications
                    )
                }
            }
            
            // Reminders Section
            item {
                SettingsSection(
                    title = "Expense Reminders",
                    action = {
                        IconButton(onClick = { showAddReminderDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Reminder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    if (uiState.appSettings.reminders.isEmpty()) {
                        Text(
                            text = "No reminders set. Tap + to add your first reminder!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // Reminder list
            items(uiState.appSettings.reminders) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    onToggle = { settingsViewModel.toggleReminder(reminder.id, !reminder.isEnabled) },
                    onEdit = {
                        selectedReminder = reminder
                        showEditReminderDialog = true
                    },
                    onDelete = {
                        selectedReminder = reminder
                        showDeleteReminderDialog = true
                    }
                )
            }
            
            // App Preferences Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsToggleItem(
                        title = "Dark Mode",
                        subtitle = "Use dark theme",
                        icon = Icons.Default.DarkMode,
                        isChecked = darkTheme,
                        onToggle = { onDarkThemeToggle() }
                    )
                }
            }
            
            // Sync Settings Section
            item {
                SettingsSection(title = "Sync & Data") {
                    SettingsToggleItem(
                        title = "Auto Sync",
                        subtitle = "Automatically sync data with server",
                        icon = Icons.Default.Sync,
                        isChecked = uiState.appSettings.autoSync,
                        onToggle = settingsViewModel::toggleAutoSync
                    )
                    
                    if (uiState.appSettings.autoSync) {
                        SettingsDropdownItem(
                            title = "Sync Frequency",
                            subtitle = uiState.appSettings.syncFrequency.displayName,
                            icon = Icons.Default.Schedule,
                            options = SyncFrequency.values().map { it.displayName },
                            selectedIndex = SyncFrequency.values().indexOf(uiState.appSettings.syncFrequency),
                            onSelectionChanged = { index ->
                                settingsViewModel.setSyncFrequency(SyncFrequency.values()[index])
                            }
                        )
                    }
                }
            }
            
            // Actions Section
            item {
                SettingsSection(title = "Actions") {
                    SettingsClickableItem(
                        title = "Test Notification",
                        subtitle = "Send a test notification immediately",
                        icon = Icons.Default.Notifications,
                        onClick = { settingsViewModel.sendTestNotification() }
                    )
                    
                    SettingsClickableItem(
                        title = "Test Expedited WorkManager",
                        subtitle = "Test immediate expedited WorkManager job",
                        icon = Icons.Default.Bolt,
                        onClick = { settingsViewModel.testExpeditedWorkManager() }
                    )
                    
                    SettingsClickableItem(
                        title = "Test 30s WorkManager",
                        subtitle = "Schedule WorkManager notification in 30 seconds",
                        icon = Icons.Default.Timer,
                        onClick = { settingsViewModel.testWorkManagerNotification() }
                    )
                    
                    SettingsClickableItem(
                        title = "Test 2min WorkManager",
                        subtitle = "Schedule WorkManager notification in 2 minutes",
                        icon = Icons.Default.Schedule,
                        onClick = { settingsViewModel.testTwoMinuteNotification() }
                    )
                    
                    SettingsClickableItem(
                        title = "Test Alarm (1min)",
                        subtitle = "Test reliable background alarm notification",
                        icon = Icons.Default.Alarm,
                        onClick = { settingsViewModel.testAlarmNotification() }
                    )
                    
                    SettingsClickableItem(
                        title = "Debug Work Status",
                        subtitle = "Check scheduled reminders status",
                        icon = Icons.Default.BugReport,
                        onClick = { settingsViewModel.debugWorkManager() }
                    )
                    
                    SettingsClickableItem(
                        title = "Reset Settings",
                        subtitle = "Reset all settings to default values",
                        icon = Icons.Default.RestartAlt,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = settingsViewModel::resetSettings
                    )
                }
            }
        }
    }
    
    // Add Reminder Dialog
    if (showAddReminderDialog) {
        AddEditReminderDialog(
            title = "Add Reminder",
            reminder = null,
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminder ->
                settingsViewModel.addReminder(reminder)
                showAddReminderDialog = false
            }
        )
    }
    
    // Edit Reminder Dialog
    if (showEditReminderDialog) {
        AddEditReminderDialog(
            title = "Edit Reminder",
            reminder = selectedReminder,
            onDismiss = { showEditReminderDialog = false },
            onSave = { reminder ->
                settingsViewModel.updateReminder(reminder)
                showEditReminderDialog = false
            }
        )
    }
    
    // Delete Reminder Dialog
    if (showDeleteReminderDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteReminderDialog = false },
            title = { Text("Delete Reminder") },
            text = { 
                Text("Are you sure you want to delete '${selectedReminder?.name}'? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedReminder?.let { settingsViewModel.removeReminder(it.id) }
                        showDeleteReminderDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            action?.invoke()
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: ReminderSettings,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reminder info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = reminder.time.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder.message != "💰 Time to log your expenses! Don't forget to track your spending for today.") {
                    Text(
                        text = reminder.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            
            // Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Reminder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reminder",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.menuAnchor()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Option"
                )
            }
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 200.dp) // Minimum width for better readability
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = option,
                                modifier = Modifier.padding(vertical = 8.dp) // More vertical padding for better touch targets
                            ) 
                        },
                        onClick = {
                            onSelectionChanged(index)
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp) // Minimum height for better touch targets
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}