package com.example.spenttracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.spenttracker.domain.model.ReminderSettings
import com.example.spenttracker.receiver.NotificationAlarmReceiver
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Alarm-based notification scheduler for reliable background notifications
 * Uses AlarmManager for exact timing even when app is closed
 */
@Singleton
class AlarmNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TAG = "AlarmNotificationScheduler"
        private const val BASE_REQUEST_CODE = 1000
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Schedule reminders using AlarmManager for better background reliability
     */
    fun scheduleReminders(reminders: List<ReminderSettings>) {
        try {
            Log.d(TAG, "Scheduling ${reminders.size} reminders via AlarmManager")
            
            // Cancel all existing alarms first
            cancelAllReminders()
            
            // Schedule each enabled reminder
            reminders.filter { it.isEnabled }.forEachIndexed { index, reminder ->
                scheduleReminderAlarm(reminder, BASE_REQUEST_CODE + index)
            }
            
            Log.i(TAG, "Successfully scheduled ${reminders.filter { it.isEnabled }.size} alarm-based reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm-based reminders", e)
        }
    }
    
    /**
     * Schedule a single reminder using AlarmManager
     */
    private fun scheduleReminderAlarm(reminder: ReminderSettings, requestCode: Int) {
        try {
            val currentTime = LocalDateTime.now()
            val reminderTime = LocalTime.of(reminder.time.hour, reminder.time.minute)
            
            // Calculate next occurrence of this time
            var nextReminderTime = currentTime.with(reminderTime)
            
            // If it's already past this time today, schedule for tomorrow
            if (currentTime.isAfter(nextReminderTime)) {
                nextReminderTime = nextReminderTime.plusDays(1)
            }
            
            val triggerTimeMillis = nextReminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            Log.d(TAG, "Scheduling alarm for '${reminder.name}' at $nextReminderTime")
            
            // Create intent for alarm receiver
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                putExtra("reminder_id", reminder.id)
                putExtra("reminder_name", reminder.name)
                putExtra("reminder_message", reminder.message)
                putExtra("reminder_hour", reminder.time.hour)
                putExtra("reminder_minute", reminder.time.minute)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle for better reliability
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Alarm scheduled for '${reminder.name}' with request code $requestCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for '${reminder.name}'", e)
        }
    }
    
    /**
     * Cancel all scheduled alarms
     */
    fun cancelAllReminders() {
        try {
            Log.d(TAG, "Canceling all alarm-based reminders")
            
            // Cancel up to 10 possible alarms (should be enough for most use cases)
            for (requestCode in BASE_REQUEST_CODE until BASE_REQUEST_CODE + 10) {
                val intent = Intent(context, NotificationAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                
                pendingIntent?.let {
                    alarmManager.cancel(it)
                    it.cancel()
                }
            }
            
            Log.i(TAG, "All alarm-based reminders canceled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm-based reminders", e)
        }
    }
    
    /**
     * Schedule a test alarm for debugging
     */
    fun scheduleTestAlarm(delayMinutes: Int = 1) {
        try {
            Log.d(TAG, "Scheduling test alarm in $delayMinutes minutes")
            
            val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000)
            
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                putExtra("reminder_id", "test_alarm_${System.currentTimeMillis()}")
                putExtra("reminder_name", "Test Alarm")
                putExtra("reminder_message", "⏰ This is a test alarm notification!")
                putExtra("is_test", true)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                999, // Special request code for test
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            Log.i(TAG, "Test alarm scheduled for $delayMinutes minutes from now")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test alarm", e)
        }
    }
    
    /**
     * Check if the app can schedule exact alarms
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Always available on older versions
        }
    }
    
    /**
     * Request exact alarm permission (Android 12+)
     */
    fun requestExactAlarmPermission(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            null
        }
    }
    
    /**
     * Request battery optimization exemption
     */
    fun requestIgnoreBatteryOptimization(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }
}