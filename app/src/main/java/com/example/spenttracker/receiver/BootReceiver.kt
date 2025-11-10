package com.example.spenttracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.spenttracker.util.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Boot Receiver to reschedule notifications after device restart
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var notificationScheduler: NotificationScheduler
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent?.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed - rescheduling notifications")
            
            // Use goAsync() for longer running operations
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    // TODO: Load saved reminder settings from repository
                    // For now, just reschedule the default daily reminder
                    notificationScheduler.scheduleDailyExpenseReminder()
                    
                    Log.i(TAG, "Notifications rescheduled after boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule notifications", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}