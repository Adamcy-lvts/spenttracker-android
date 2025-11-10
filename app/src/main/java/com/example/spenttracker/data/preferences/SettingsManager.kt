package com.example.spenttracker.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.spenttracker.domain.model.AppSettings
import com.example.spenttracker.domain.model.ReminderSettings
import com.example.spenttracker.domain.model.SyncFrequency
import com.example.spenttracker.domain.model.getDefaultReminders
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings Manager for handling app preferences and reminder settings
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson = Gson()
) {
    
    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "spent_tracker_settings"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_SYNC_FREQUENCY = "sync_frequency"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _appSettings = MutableStateFlow(loadAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    
    /**
     * Load app settings from SharedPreferences
     */
    private fun loadAppSettings(): AppSettings {
        return try {
            val reminders = loadReminders()
            val notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
            val darkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
            val autoSync = sharedPreferences.getBoolean(KEY_AUTO_SYNC, true)
            val syncFrequencyName = sharedPreferences.getString(KEY_SYNC_FREQUENCY, SyncFrequency.HOURLY.name)
            val syncFrequency = try {
                SyncFrequency.valueOf(syncFrequencyName ?: SyncFrequency.HOURLY.name)
            } catch (e: IllegalArgumentException) {
                SyncFrequency.HOURLY
            }
            
            AppSettings(
                reminders = reminders,
                notificationsEnabled = notificationsEnabled,
                darkMode = darkMode,
                autoSync = autoSync,
                syncFrequency = syncFrequency
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading app settings, using defaults", e)
            AppSettings()
        }
    }
    
    /**
     * Load reminder settings from SharedPreferences
     */
    private fun loadReminders(): List<ReminderSettings> {
        return try {
            val remindersJson = sharedPreferences.getString(KEY_REMINDERS, null)
            if (remindersJson != null) {
                val type = object : TypeToken<List<ReminderSettingsData>>() {}.type
                val reminderDataList: List<ReminderSettingsData> = gson.fromJson(remindersJson, type)
                reminderDataList.map { it.toDomain() }
            } else {
                // Return default reminders if none saved
                getDefaultReminders()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading reminders, using defaults", e)
            getDefaultReminders()
        }
    }
    
    /**
     * Save reminder settings
     */
    fun saveReminders(reminders: List<ReminderSettings>) {
        try {
            val reminderDataList = reminders.map { ReminderSettingsData.fromDomain(it) }
            val remindersJson = gson.toJson(reminderDataList)
            sharedPreferences.edit()
                .putString(KEY_REMINDERS, remindersJson)
                .apply()
            
            // Update state flow
            _appSettings.value = _appSettings.value.copy(reminders = reminders)
            Log.d(TAG, "Reminders saved: ${reminders.size} reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving reminders", e)
        }
    }
    
    /**
     * Add a new reminder
     */
    fun addReminder(reminder: ReminderSettings) {
        val currentReminders = _appSettings.value.reminders.toMutableList()
        currentReminders.add(reminder)
        saveReminders(currentReminders)
    }
    
    /**
     * Update an existing reminder
     */
    fun updateReminder(reminder: ReminderSettings) {
        val currentReminders = _appSettings.value.reminders.toMutableList()
        val index = currentReminders.indexOfFirst { it.id == reminder.id }
        if (index != -1) {
            currentReminders[index] = reminder
            saveReminders(currentReminders)
        }
    }
    
    /**
     * Remove a reminder
     */
    fun removeReminder(reminderId: String) {
        val currentReminders = _appSettings.value.reminders.toMutableList()
        currentReminders.removeAll { it.id == reminderId }
        saveReminders(currentReminders)
    }
    
    /**
     * Toggle reminder enabled/disabled
     */
    fun toggleReminder(reminderId: String, enabled: Boolean) {
        val currentReminders = _appSettings.value.reminders.toMutableList()
        val index = currentReminders.indexOfFirst { it.id == reminderId }
        if (index != -1) {
            currentReminders[index] = currentReminders[index].copy(isEnabled = enabled)
            saveReminders(currentReminders)
        }
    }
    
    /**
     * Save notifications enabled setting
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
        _appSettings.value = _appSettings.value.copy(notificationsEnabled = enabled)
    }
    
    /**
     * Save dark mode setting
     */
    fun setDarkMode(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
        _appSettings.value = _appSettings.value.copy(darkMode = enabled)
    }
    
    /**
     * Save auto sync setting
     */
    fun setAutoSync(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_SYNC, enabled)
            .apply()
        _appSettings.value = _appSettings.value.copy(autoSync = enabled)
    }
    
    /**
     * Save sync frequency setting
     */
    fun setSyncFrequency(frequency: SyncFrequency) {
        sharedPreferences.edit()
            .putString(KEY_SYNC_FREQUENCY, frequency.name)
            .apply()
        _appSettings.value = _appSettings.value.copy(syncFrequency = frequency)
    }
    
    /**
     * Get enabled reminders only
     */
    fun getEnabledReminders(): List<ReminderSettings> {
        return _appSettings.value.reminders.filter { it.isEnabled }
    }
    
    /**
     * Reset settings to default
     */
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
        _appSettings.value = AppSettings()
        Log.d(TAG, "Settings reset to defaults")
    }
}

/**
 * Data class for JSON serialization of ReminderSettings
 * (LocalTime cannot be directly serialized by Gson)
 */
private data class ReminderSettingsData(
    val id: String,
    val name: String,
    val timeString: String, // Store time as string
    val isEnabled: Boolean,
    val message: String,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): ReminderSettings {
        val time = try {
            LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME)
        } catch (e: Exception) {
            LocalTime.of(22, 0) // Default to 10 PM if parsing fails
        }
        
        return ReminderSettings(
            id = id,
            name = name,
            time = time,
            isEnabled = isEnabled,
            message = message,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomain(reminder: ReminderSettings): ReminderSettingsData {
            return ReminderSettingsData(
                id = reminder.id,
                name = reminder.name,
                timeString = reminder.time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                isEnabled = reminder.isEnabled,
                message = reminder.message,
                createdAt = reminder.createdAt,
                updatedAt = reminder.updatedAt
            )
        }
    }
}