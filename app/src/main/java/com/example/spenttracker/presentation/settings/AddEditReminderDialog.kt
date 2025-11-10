package com.example.spenttracker.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.spenttracker.domain.model.ReminderSettings
import com.example.spenttracker.domain.model.getPresetReminderMessages
import com.example.spenttracker.domain.model.getPresetReminderTimes
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderDialog(
    title: String,
    reminder: ReminderSettings?,
    onDismiss: () -> Unit,
    onSave: (ReminderSettings) -> Unit
) {
    var name by remember { mutableStateOf(reminder?.name ?: "") }
    var selectedTime by remember { mutableStateOf(reminder?.time ?: LocalTime.of(22, 0)) }
    var message by remember { mutableStateOf(reminder?.message ?: "💰 Time to log your expenses! Don't forget to track your spending for today.") }
    var isEnabled by remember { mutableStateOf(reminder?.isEnabled ?: true) }
    
    // Time picker states
    var showTimePicker by remember { mutableStateOf(false) }
    var showPresetTimes by remember { mutableStateOf(false) }
    var showPresetMessages by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // Reminder Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reminder Name") },
                    placeholder = { Text("e.g., Evening Reminder") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Time Selection
                Column {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(selectedTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                        }
                        
                        OutlinedButton(
                            onClick = { showPresetTimes = true }
                        ) {
                            Text("Presets")
                        }
                    }
                }
                
                // Custom Message
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.labelLarge
                        )
                        
                        TextButton(onClick = { showPresetMessages = true }) {
                            Text("Use Preset")
                        }
                    }
                    
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Custom reminder message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                
                // Enabled Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled")
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val newReminder = ReminderSettings(
                                id = reminder?.id ?: UUID.randomUUID().toString(),
                                name = name.takeIf { it.isNotBlank() } ?: "Reminder",
                                time = selectedTime,
                                isEnabled = isEnabled,
                                message = message,
                                createdAt = reminder?.createdAt ?: "",
                                updatedAt = ""
                            )
                            onSave(newReminder)
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (reminder == null) "Add" else "Save")
                    }
                }
            }
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
    
    // Preset Times Dialog
    if (showPresetTimes) {
        PresetSelectionDialog(
            title = "Select Time",
            items = getPresetReminderTimes(),
            onItemSelected = { (_, time) ->
                selectedTime = time
                showPresetTimes = false
            },
            onDismiss = { showPresetTimes = false }
        )
    }
    
    // Preset Messages Dialog
    if (showPresetMessages) {
        PresetMessageDialog(
            messages = getPresetReminderMessages(),
            onMessageSelected = { selectedMessage ->
                message = selectedMessage
                showPresetMessages = false
            },
            onDismiss = { showPresetMessages = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // Increased height for better AM/PM visibility
            ) {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PresetSelectionDialog(
    title: String,
    items: List<Pair<String, LocalTime>>,
    onItemSelected: (Pair<String, LocalTime>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp) // Limit height to prevent overflow
            ) {
                items.forEach { item ->
                    TextButton(
                        onClick = { onItemSelected(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp) // Better touch targets
                    ) {
                        Text(
                            text = item.first,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PresetMessageDialog(
    messages: List<String>,
    onMessageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Message") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()) // Make scrollable for many messages
            ) {
                messages.forEach { message ->
                    TextButton(
                        onClick = { onMessageSelected(message) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp) // Larger touch targets for multi-line text
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            maxLines = 2
                        )
                    }
                    Divider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}