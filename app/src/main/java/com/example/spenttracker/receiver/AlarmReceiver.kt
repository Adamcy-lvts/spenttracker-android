package com.example.spenttracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.spenttracker.util.ExpenseNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AlarmManager receiver for backup scheduling
 * Alternative to WorkManager for time-critical notifications
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var notificationManager: ExpenseNotificationManager
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_EXPENSE_REMINDER = "com.example.spenttracker.EXPENSE_REMINDER"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_EXPENSE_REMINDER) {
            Log.d(TAG, "Alarm triggered - showing expense reminder")
            
            try {
                val reminderMessage = intent.getStringExtra("reminder_message") 
                    ?: "Don't forget to track your spending for today!"
                
                if (notificationManager.hasNotificationPermission()) {
                    notificationManager.showDailyExpenseReminder(reminderMessage)
                    Log.i(TAG, "Alarm notification shown successfully")
                } else {
                    Log.w(TAG, "No notification permission - skipping alarm")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show alarm notification", e)
            }
        }
    }
}