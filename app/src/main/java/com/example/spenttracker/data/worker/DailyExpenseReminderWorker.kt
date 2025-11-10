package com.example.spenttracker.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.spenttracker.util.ExpenseNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker for Daily Expense Reminders
 * Executes the actual notification display when scheduled time arrives
 */
class DailyExpenseReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    private val notificationManager = ExpenseNotificationManager(context)
    
    companion object {
        const val TAG = "DailyExpenseReminderWorker"
        const val WORK_NAME = "daily_expense_reminder"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "DailyExpenseReminderWorker started")
            
            // Extract reminder details from input data
            val reminderId = inputData.getString("reminder_id") ?: "default"
            val reminderName = inputData.getString("reminder_name") ?: "Daily Expense Reminder"
            val reminderMessage = inputData.getString("reminder_message") 
                ?: "Don't forget to track your spending for today!"
            val isTest = inputData.getBoolean("is_test", false)
            
            Log.d(TAG, "Processing reminder: $reminderName (ID: $reminderId, Test: $isTest)")
            
            // Check if we have notification permission
            if (!notificationManager.hasNotificationPermission()) {
                Log.w(TAG, "No notification permission - skipping reminder")
                return@withContext Result.failure(
                    workDataOf("error" to "No notification permission")
                )
            }
            
            // Show the notification
            try {
                notificationManager.showDailyExpenseReminder(reminderMessage)
                Log.i(TAG, "Notification shown successfully for: $reminderName")
                
                // Return success with output data
                Result.success(
                    workDataOf(
                        "reminder_id" to reminderId,
                        "reminder_name" to reminderName,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification", e)
                Result.failure(
                    workDataOf("error" to "Failed to show notification: ${e.message}")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed with exception", e)
            Result.failure(
                workDataOf("error" to "Worker exception: ${e.message}")
            )
        }
    }
}