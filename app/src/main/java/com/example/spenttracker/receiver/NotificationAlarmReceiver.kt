package com.example.spenttracker.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.spenttracker.util.ExpenseNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Broadcast receiver for handling alarm-based notifications
 * Receives alarms and shows notifications even when app is closed
 */
@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var notificationManager: ExpenseNotificationManager
    
    companion object {
        const val TAG = "NotificationAlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Alarm received!")
            
            // Extract reminder data from intent
            val reminderId = intent.getStringExtra("reminder_id") ?: "unknown"
            val reminderName = intent.getStringExtra("reminder_name") ?: "Expense Reminder"
            val reminderMessage = intent.getStringExtra("reminder_message") ?: "Don't forget to track your expenses!"
            val isTest = intent.getBooleanExtra("is_test", false)
            val hour = intent.getIntExtra("reminder_hour", -1)
            val minute = intent.getIntExtra("reminder_minute", -1)
            
            Log.d(TAG, "Processing alarm: $reminderName (ID: $reminderId, Test: $isTest)")
            
            // Show the notification
            try {
                notificationManager.showDailyExpenseReminder(reminderMessage)
                Log.i(TAG, "Alarm notification shown successfully: $reminderName")
                
                // If this is not a test and we have valid time, schedule for tomorrow
                if (!isTest && hour >= 0 && minute >= 0) {
                    scheduleNextAlarm(context, intent, hour, minute)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show alarm notification", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in alarm receiver", e)
        }
    }
    
    /**
     * Schedule the same alarm for tomorrow (for daily recurring)
     */
    private fun scheduleNextAlarm(context: Context, originalIntent: Intent, hour: Int, minute: Int) {
        try {
            Log.d(TAG, "Scheduling next alarm for tomorrow at $hour:$minute")
            
            // Calculate tomorrow at the same time
            val tomorrow = LocalDateTime.now().plusDays(1).with(LocalTime.of(hour, minute))
            val triggerTimeMillis = tomorrow.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Create new intent with same data
            val nextIntent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                putExtra("reminder_id", originalIntent.getStringExtra("reminder_id"))
                putExtra("reminder_name", originalIntent.getStringExtra("reminder_name"))
                putExtra("reminder_message", originalIntent.getStringExtra("reminder_message"))
                putExtra("reminder_hour", hour)
                putExtra("reminder_minute", minute)
                putExtra("is_test", false) // Make sure it's not marked as test
            }
            
            // Use the same request code from the reminder ID hash
            val requestCode = (originalIntent.getStringExtra("reminder_id")?.hashCode() ?: 0).let { 
                1000 + Math.abs(it % 1000) // Ensure positive and within reasonable range
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Schedule for tomorrow
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            
            Log.d(TAG, "Next alarm scheduled for tomorrow at $tomorrow")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule next alarm", e)
        }
    }
}