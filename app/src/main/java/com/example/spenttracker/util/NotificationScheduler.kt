package com.example.spenttracker.util

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.spenttracker.data.worker.DailyExpenseReminderWorker
import com.example.spenttracker.domain.model.ReminderSettings
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Notification Scheduler for Daily Expense Reminders
 * Handles scheduling and canceling the daily expense reminder notifications
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        const val TAG = "NotificationScheduler"
        private const val REMINDER_TIME_HOUR = 22 // 10 PM
        private const val REMINDER_TIME_MINUTE = 0
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule daily expense reminder at 10 PM
     */
    fun scheduleDailyExpenseReminder() {
        try {
            Log.d(TAG, "Scheduling daily expense reminder at 10:00 PM")
            
            val currentTime = LocalDateTime.now()
            val reminderTime = LocalTime.of(REMINDER_TIME_HOUR, REMINDER_TIME_MINUTE)
            
            // Calculate initial delay until next 10 PM
            var nextReminderTime = currentTime.with(reminderTime)
            
            // If it's already past 10 PM today, schedule for tomorrow
            if (currentTime.isAfter(nextReminderTime)) {
                nextReminderTime = nextReminderTime.plusDays(1)
            }
            
            val initialDelayMinutes = java.time.Duration.between(currentTime, nextReminderTime).toMinutes()
            
            Log.d(TAG, "Initial delay until next reminder: $initialDelayMinutes minutes")
            Log.d(TAG, "Next reminder scheduled for: $nextReminderTime")
            
            // IMPORTANT: Use minimal constraints for better reliability
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            // Create the recurring work request
            val dailyReminderWork = PeriodicWorkRequestBuilder<DailyExpenseReminderWorker>(
                24, // Repeat every 24 hours (more explicit than 1 day)
                TimeUnit.HOURS,
                15, // Flex interval of 15 minutes
                TimeUnit.MINUTES
            )
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(DailyExpenseReminderWorker.TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, // Use exponential backoff instead of linear
                    WorkRequest.MIN_BACKOFF_MILLIS, // Use minimum backoff
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // Schedule the work, using CANCEL_AND_REENQUEUE for better reliability
            workManager.enqueueUniquePeriodicWork(
                DailyExpenseReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, // Changed from REPLACE
                dailyReminderWork
            )
            
            Log.i(TAG, "Daily expense reminder scheduled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule daily expense reminder", e)
        }
    }
    
    /**
     * Cancel daily expense reminder
     */
    fun cancelDailyExpenseReminder() {
        try {
            Log.d(TAG, "Canceling daily expense reminder")
            workManager.cancelUniqueWork(DailyExpenseReminderWorker.WORK_NAME)
            Log.i(TAG, "Daily expense reminder canceled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel daily expense reminder", e)
        }
    }
    
    /**
     * Check if daily reminder is currently scheduled
     */
    fun isDailyReminderScheduled(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(DailyExpenseReminderWorker.WORK_NAME).get()
            workInfos.any { workInfo -> 
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING 
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking reminder schedule status", e)
            false
        }
    }
    
    /**
     * Get status of daily reminder scheduling
     */
    fun getDailyReminderStatus(): String {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(DailyExpenseReminderWorker.WORK_NAME).get()
            when {
                workInfos.isEmpty() -> "Not Scheduled"
                workInfos.any { it.state == WorkInfo.State.ENQUEUED } -> "Scheduled"
                workInfos.any { it.state == WorkInfo.State.RUNNING } -> "Running"
                workInfos.any { it.state == WorkInfo.State.SUCCEEDED } -> "Last Run Successful"
                workInfos.any { it.state == WorkInfo.State.FAILED } -> "Last Run Failed"
                workInfos.any { it.state == WorkInfo.State.CANCELLED } -> "Cancelled"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting reminder status", e)
            "Error"
        }
    }
    
    /**
     * Schedule multiple daily reminders
     */
    fun scheduleMultipleReminders(reminders: List<ReminderSettings>) {
        try {
            Log.d(TAG, "Scheduling ${reminders.size} reminders")
            
            // Cancel all existing reminders first
            cancelAllReminders()
            
            // Schedule each enabled reminder
            reminders.filter { it.isEnabled }.forEach { reminder ->
                scheduleReminderForTime(reminder)
            }
            
            Log.i(TAG, "Successfully scheduled ${reminders.filter { it.isEnabled }.size} reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule multiple reminders", e)
        }
    }
    
    /**
     * Schedule immediate expedited test (no delay)
     */
    fun scheduleImmediateExpeditedTest() {
        try {
            Log.d(TAG, "Scheduling immediate expedited test")
            
            val inputData = Data.Builder()
                .putString("reminder_id", "expedited_test_${System.currentTimeMillis()}")
                .putString("reminder_name", "Immediate Expedited Test")
                .putString("reminder_message", "🚀 Expedited WorkManager test - should run immediately!")
                .putBoolean("is_test", true)
                .build()
            
            // Expedited work - no delay allowed
            val expeditedWork = OneTimeWorkRequestBuilder<DailyExpenseReminderWorker>()
                .setInputData(inputData)
                .addTag("expedited_test")
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            
            workManager.enqueue(expeditedWork)
            
            Log.i(TAG, "Immediate expedited test scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule expedited test", e)
        }
    }
    
    /**
     * TEST: Schedule delayed test reminder for debugging
     * This version uses OneTimeWorkRequest which is more reliable for testing
     */
    fun scheduleTestReminder(delaySeconds: Long = 30) {
        try {
            Log.d(TAG, "Scheduling TEST reminder with ${delaySeconds}s delay")
            
            // Create work data with test message
            val inputData = Data.Builder()
                .putString("reminder_id", "test_${System.currentTimeMillis()}")
                .putString("reminder_name", "Test Reminder")
                .putString("reminder_message", "This is a test notification!")
                .putBoolean("is_test", true)
                .build()
            
            // No constraints for test - we want it to run no matter what
            val testReminderWork = OneTimeWorkRequestBuilder<DailyExpenseReminderWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("test_reminder")
                // NOTE: Cannot use setExpedited() with setInitialDelay() - they are mutually exclusive
                .build()
            
            // Schedule the work
            workManager.enqueue(testReminderWork)
            
            // Observe the work status for debugging
            workManager.getWorkInfoByIdLiveData(testReminderWork.id).observeForever { workInfo ->
                if (workInfo != null) {
                    Log.d(TAG, "Test reminder status: ${workInfo.state}")
                    if (workInfo.state == WorkInfo.State.FAILED) {
                        Log.e(TAG, "Test reminder failed! Output: ${workInfo.outputData}")
                    }
                }
            }
            
            Log.i(TAG, "TEST reminder scheduled in ${delaySeconds} seconds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule TEST reminder", e)
        }
    }
    
    /**
     * Schedule a test reminder with custom settings
     */
    fun scheduleTestReminder(reminder: ReminderSettings, delayMinutes: Long) {
        try {
            Log.d(TAG, "Scheduling TEST reminder '${reminder.name}' with ${delayMinutes}min delay")
            
            // Create work data with reminder details
            val inputData = Data.Builder()
                .putString("reminder_id", reminder.id)
                .putString("reminder_name", reminder.name)
                .putString("reminder_message", reminder.message)
                .putBoolean("is_test", true)
                .build()
            
            // Create one-time work request for testing
            val testReminderWork = OneTimeWorkRequestBuilder<DailyExpenseReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(inputData)
                .addTag(DailyExpenseReminderWorker.TAG)
                .addTag("test_reminder_${reminder.id}")
                // NOTE: Cannot use setExpedited() with setInitialDelay() - they are mutually exclusive
                .build()
            
            // Schedule the work with unique name
            workManager.enqueueUniqueWork(
                "test_reminder_${reminder.id}",
                ExistingWorkPolicy.REPLACE,
                testReminderWork
            )
            
            Log.i(TAG, "TEST reminder scheduled: ${reminder.name} in ${delayMinutes} minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule TEST reminder: ${reminder.name}", e)
        }
    }

    /**
     * Schedule a single reminder for a specific time
     */
    private fun scheduleReminderForTime(reminder: ReminderSettings) {
        try {
            val currentTime = LocalDateTime.now()
            val reminderTime = LocalTime.of(reminder.time.hour, reminder.time.minute)
            
            // Calculate initial delay until next occurrence of this time
            var nextReminderTime = currentTime.with(reminderTime)
            
            // If it's already past this time today, schedule for tomorrow
            if (currentTime.isAfter(nextReminderTime)) {
                nextReminderTime = nextReminderTime.plusDays(1)
            }
            
            val initialDelayMinutes = java.time.Duration.between(currentTime, nextReminderTime).toMinutes()
            
            Log.d(TAG, "Scheduling reminder '${reminder.name}' with ${initialDelayMinutes}min delay")
            
            // Use minimal constraints for better reliability
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            
            // Create work data with reminder details
            val inputData = Data.Builder()
                .putString("reminder_id", reminder.id)
                .putString("reminder_name", reminder.name)
                .putString("reminder_message", reminder.message)
                .build()
            
            // Create the recurring work request
            val reminderWork = PeriodicWorkRequestBuilder<DailyExpenseReminderWorker>(
                24, TimeUnit.HOURS, // Use 24 hours instead of 1 day
                15, TimeUnit.MINUTES // 15-minute flex window
            )
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(DailyExpenseReminderWorker.TAG)
                .addTag("reminder_${reminder.id}")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // Schedule the work with unique name
            workManager.enqueueUniquePeriodicWork(
                "reminder_${reminder.id}",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, // Changed from REPLACE
                reminderWork
            )
            
            Log.d(TAG, "Scheduled reminder: ${reminder.name} at ${reminder.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder: ${reminder.name}", e)
        }
    }
    
    /**
     * Cancel all reminders
     */
    fun cancelAllReminders() {
        try {
            Log.d(TAG, "Canceling all reminders")
            workManager.cancelAllWorkByTag(DailyExpenseReminderWorker.TAG)
            Log.i(TAG, "All reminders canceled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all reminders", e)
        }
    }
    
    /**
     * Cancel a specific reminder
     */
    fun cancelReminder(reminderId: String) {
        try {
            Log.d(TAG, "Canceling reminder: $reminderId")
            workManager.cancelUniqueWork("reminder_$reminderId")
            Log.i(TAG, "Reminder canceled successfully: $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reminder: $reminderId", e)
        }
    }
    
    /**
     * Get status of all scheduled reminders
     */
    fun getAllReminderStatuses(): List<Pair<String, String>> {
        return try {
            val workInfos = workManager.getWorkInfosByTag(DailyExpenseReminderWorker.TAG).get()
            workInfos.map { workInfo ->
                val reminderId = workInfo.tags.find { it.startsWith("reminder_") }?.removePrefix("reminder_") ?: "unknown"
                val status = when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> "Scheduled"
                    WorkInfo.State.RUNNING -> "Running"
                    WorkInfo.State.SUCCEEDED -> "Completed"
                    WorkInfo.State.FAILED -> "Failed"
                    WorkInfo.State.BLOCKED -> "Blocked"
                    WorkInfo.State.CANCELLED -> "Cancelled"
                }
                reminderId to status
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting reminder statuses", e)
            emptyList()
        }
    }
    
    /**
     * Debug method to check WorkManager configuration
     */
    fun debugWorkManager() {
        try {
            Log.d(TAG, "=== WorkManager Debug Info ===")
            
            // Get all work info
            val allWork = workManager.getWorkInfosByTag(DailyExpenseReminderWorker.TAG).get()
            Log.d(TAG, "Total scheduled works: ${allWork.size}")
            
            allWork.forEach { workInfo ->
                Log.d(TAG, "Work ID: ${workInfo.id}")
                Log.d(TAG, "  State: ${workInfo.state}")
                Log.d(TAG, "  Tags: ${workInfo.tags}")
                Log.d(TAG, "  Run attempt count: ${workInfo.runAttemptCount}")
                Log.d(TAG, "  Output: ${workInfo.outputData}")
            }
            
            // Check if WorkManager is properly initialized
            val configuration = WorkManager.getInstance(context).configuration
            Log.d(TAG, "WorkManager min job scheduler ID: ${configuration.minJobSchedulerId}")
            Log.d(TAG, "WorkManager max job scheduler ID: ${configuration.maxJobSchedulerId}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error debugging WorkManager", e)
        }
    }
}