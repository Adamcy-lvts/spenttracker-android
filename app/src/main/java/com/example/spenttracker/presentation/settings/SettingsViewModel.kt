package com.example.spenttracker.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spenttracker.data.preferences.SettingsManager
import com.example.spenttracker.domain.model.AppSettings
import com.example.spenttracker.domain.model.ReminderSettings
import com.example.spenttracker.domain.model.SyncFrequency
import com.example.spenttracker.util.NotificationScheduler
import com.example.spenttracker.util.AlarmNotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Settings ViewModel - Manages app settings and preferences
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val notificationScheduler: NotificationScheduler,
    private val alarmScheduler: AlarmNotificationScheduler,
    private val notificationManager: com.example.spenttracker.util.ExpenseNotificationManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        // Observe settings changes
        viewModelScope.launch {
            settingsManager.appSettings.collect { appSettings ->
                _uiState.value = _uiState.value.copy(
                    appSettings = appSettings,
                    isLoading = false
                )
                
                // Update notification scheduler when reminders change
                updateNotificationScheduler()
            }
        }
    }
    
    /**
     * Toggle notifications on/off
     */
    fun toggleNotifications() {
        viewModelScope.launch {
            val currentValue = _uiState.value.appSettings.notificationsEnabled
            settingsManager.setNotificationsEnabled(!currentValue)
            
            // Update notification scheduler
            updateNotificationScheduler()
            
            showSuccessMessage(
                if (!currentValue) "Notifications enabled" else "Notifications disabled"
            )
        }
    }
    
    /**
     * Add a new reminder
     */
    fun addReminder(reminder: ReminderSettings) {
        viewModelScope.launch {
            try {
                settingsManager.addReminder(reminder)
                Log.d(TAG, "Added reminder: ${reminder.name} at ${reminder.time}")
                showSuccessMessage("Reminder added: ${reminder.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding reminder", e)
                showErrorMessage("Failed to add reminder")
            }
        }
    }
    
    /**
     * Update an existing reminder
     */
    fun updateReminder(reminder: ReminderSettings) {
        viewModelScope.launch {
            try {
                settingsManager.updateReminder(reminder)
                Log.d(TAG, "Updated reminder: ${reminder.name} at ${reminder.time}")
                showSuccessMessage("Reminder updated: ${reminder.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reminder", e)
                showErrorMessage("Failed to update reminder")
            }
        }
    }
    
    /**
     * Remove a reminder
     */
    fun removeReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                settingsManager.removeReminder(reminderId)
                Log.d(TAG, "Removed reminder: $reminderId")
                showSuccessMessage("Reminder deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing reminder", e)
                showErrorMessage("Failed to delete reminder")
            }
        }
    }
    
    /**
     * Toggle reminder enabled/disabled
     */
    fun toggleReminder(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsManager.toggleReminder(reminderId, enabled)
                Log.d(TAG, "Toggled reminder $reminderId to $enabled")
                showSuccessMessage(
                    if (enabled) "Reminder enabled" else "Reminder disabled"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling reminder", e)
                showErrorMessage("Failed to toggle reminder")
            }
        }
    }
    
    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        viewModelScope.launch {
            val currentValue = _uiState.value.appSettings.darkMode
            settingsManager.setDarkMode(!currentValue)
            showSuccessMessage(
                if (!currentValue) "Dark mode enabled" else "Light mode enabled"
            )
        }
    }
    
    /**
     * Toggle auto sync
     */
    fun toggleAutoSync() {
        viewModelScope.launch {
            val currentValue = _uiState.value.appSettings.autoSync
            settingsManager.setAutoSync(!currentValue)
            showSuccessMessage(
                if (!currentValue) "Auto sync enabled" else "Auto sync disabled"
            )
        }
    }
    
    /**
     * Set sync frequency
     */
    fun setSyncFrequency(frequency: SyncFrequency) {
        viewModelScope.launch {
            settingsManager.setSyncFrequency(frequency)
            showSuccessMessage("Sync frequency updated to ${frequency.displayName}")
        }
    }
    
    /**
     * Reset all settings to default
     */
    fun resetSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Cancel all existing notifications
                notificationScheduler.cancelDailyExpenseReminder()
                
                // Reset settings
                settingsManager.resetToDefaults()
                
                // Re-schedule notifications with default settings
                updateNotificationScheduler()
                
                showSuccessMessage("Settings reset to defaults")
                Log.d(TAG, "Settings reset to defaults")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting settings", e)
                showErrorMessage("Failed to reset settings")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * Update notification scheduler based on current settings
     * Uses AlarmManager for reliable background scheduling
     */
    private fun updateNotificationScheduler() {
        viewModelScope.launch {
            try {
                val appSettings = _uiState.value.appSettings
                
                if (appSettings.notificationsEnabled) {
                    val enabledReminders = appSettings.reminders.filter { it.isEnabled }
                    
                    if (enabledReminders.isNotEmpty()) {
                        // Use AlarmManager for reliable background scheduling
                        alarmScheduler.scheduleReminders(enabledReminders)
                        Log.d(TAG, "Scheduled ${enabledReminders.size} reminders via AlarmManager")
                        
                        // Also keep WorkManager for test functions
                        notificationScheduler.scheduleMultipleReminders(enabledReminders)
                        Log.d(TAG, "Backup WorkManager schedule: ${enabledReminders.size} reminders")
                    } else {
                        // No enabled reminders, cancel all
                        alarmScheduler.cancelAllReminders()
                        notificationScheduler.cancelAllReminders()
                        Log.d(TAG, "No enabled reminders, cancelled all notifications")
                    }
                } else {
                    // Notifications disabled, cancel all
                    alarmScheduler.cancelAllReminders()
                    notificationScheduler.cancelAllReminders()
                    Log.d(TAG, "Notifications disabled, cancelled all notifications")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification scheduler", e)
            }
        }
    }
    
    /**
     * Show success message
     */
    private fun showSuccessMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            errorMessage = null
        )
        
        // Clear message after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }
    
    /**
     * Show error message
     */
    private fun showErrorMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            successMessage = null
        )
        
        // Clear message after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
    
    /**
     * Clear messages manually
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
    
    /**
     * Send a test notification immediately
     */
    fun sendTestNotification() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Sending test notification")
                
                // Send test notification (channel is created in constructor)
                notificationManager.showDailyExpenseReminder("🧪 This is a test notification! Your reminder system is working correctly.")
                
                showSuccessMessage("Test notification sent!")
                Log.i(TAG, "Test notification sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send test notification", e)
                showErrorMessage("Failed to send test notification: ${e.message}")
            }
        }
    }
    
    /**
     * Debug WorkManager status
     */
    fun debugWorkManager() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Debugging WorkManager status")
                
                // Get current settings
                val appSettings = _uiState.value.appSettings
                val enabledReminders = appSettings.reminders.filter { it.isEnabled }
                
                Log.d(TAG, "Current settings:")
                Log.d(TAG, "- Notifications enabled: ${appSettings.notificationsEnabled}")
                Log.d(TAG, "- Total reminders: ${appSettings.reminders.size}")
                Log.d(TAG, "- Enabled reminders: ${enabledReminders.size}")
                
                enabledReminders.forEachIndexed { index, reminder ->
                    Log.d(TAG, "- Reminder $index: ${reminder.name} at ${reminder.time}")
                }
                
                // Call the new debug method from NotificationScheduler
                notificationScheduler.debugWorkManager()
                
                // Get reminder statuses
                val statuses = notificationScheduler.getAllReminderStatuses()
                Log.d(TAG, "WorkManager statuses:")
                statuses.forEach { (id, status) ->
                    Log.d(TAG, "- Reminder $id: $status")
                }
                
                showSuccessMessage("Work status logged - check device logs!")
                Log.i(TAG, "WorkManager debug info logged")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to debug work manager", e)
                showErrorMessage("Debug failed: ${e.message}")
            }
        }
    }
    
    /**
     * Test immediate expedited WorkManager notification
     */
    fun testExpeditedWorkManager() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing immediate expedited WorkManager notification")
                
                // Schedule an immediate expedited notification
                notificationScheduler.scheduleImmediateExpeditedTest()
                
                showSuccessMessage("Immediate expedited test scheduled! Should appear within seconds.")
                Log.i(TAG, "Immediate expedited test scheduled via WorkManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule expedited WorkManager notification", e)
                showErrorMessage("Expedited test failed: ${e.message}")
            }
        }
    }
    
    /**
     * Test 30-second WorkManager notification
     */
    fun testWorkManagerNotification() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing 30-second WorkManager notification")
                
                // Schedule a test notification for 30 seconds from now
                notificationScheduler.scheduleTestReminder(30)
                
                showSuccessMessage("30-second test notification scheduled! Check logs for status.")
                Log.i(TAG, "30-second test notification scheduled via WorkManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule test WorkManager notification", e)
                showErrorMessage("Test scheduling failed: ${e.message}")
            }
        }
    }
    
    /**
     * Test 2-minute WorkManager notification
     */
    fun testTwoMinuteNotification() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing 2-minute WorkManager notification")
                
                // Schedule a test notification for 2 minutes from now
                notificationScheduler.scheduleTestReminder(120) // 120 seconds = 2 minutes
                
                showSuccessMessage("2-minute test notification scheduled! Check logs for status.")
                Log.i(TAG, "2-minute test notification scheduled via WorkManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule 2-minute test notification", e)
                showErrorMessage("2-minute test failed: ${e.message}")
            }
        }
    }
    
    /**
     * Test alarm-based notification (1 minute delay)
     */
    fun testAlarmNotification() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing alarm-based notification in 1 minute")
                
                // Check if we can schedule exact alarms
                if (!alarmScheduler.canScheduleExactAlarms()) {
                    showErrorMessage("Exact alarm permission required for background notifications")
                    return@launch
                }
                
                // Schedule a test alarm for 1 minute from now
                alarmScheduler.scheduleTestAlarm(1) // 1 minute
                
                showSuccessMessage("Alarm test scheduled! Notification will appear in 1 minute even if app is closed.")
                Log.i(TAG, "Test alarm scheduled for 1 minute from now")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule test alarm", e)
                showErrorMessage("Alarm test failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get reminder statistics
     */
    fun getReminderStats(): String {
        val appSettings = _uiState.value.appSettings
        val total = appSettings.reminders.size
        val enabled = appSettings.reminders.count { it.isEnabled }
        return "$enabled of $total reminders enabled"
    }
}