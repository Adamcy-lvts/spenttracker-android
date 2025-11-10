package com.example.spenttracker.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spenttracker.MainActivity
import com.example.spenttracker.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Manager for Expense Reminders
 * Handles creating and showing expense reminder notifications
 */
@Singleton
class ExpenseNotificationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        const val CHANNEL_ID = "expense_reminders"
        const val CHANNEL_NAME = "Expense Reminders"
        const val CHANNEL_DESCRIPTION = "Daily reminders to log your expenses"
        const val NOTIFICATION_ID = 1001
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            val systemNotificationManager = 
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show daily expense reminder notification
     */
    fun showDailyExpenseReminder(customMessage: String? = null) {
        if (!hasNotificationPermission()) {
            return
        }
        
        // Create intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Use custom message or default
        val message = customMessage ?: "Don't forget to track your spending for today. It only takes a minute!"
        val title = if (customMessage != null && customMessage.contains("💰")) {
            customMessage.split("!")[0] + "!"
        } else {
            "💰 Time to log your expenses!"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dashboard_24) // Using existing icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message Consistent tracking helps you stay on top of your finances. 📊")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            // Show toast message with the same message as notification
            showToastMessage(message)
            
        } catch (e: SecurityException) {
            // Handle case where notification permission was revoked
            android.util.Log.w("ExpenseNotificationManager", "Notification permission denied", e)
        }
    }
    
    /**
     * Check if the app has notification permission
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    /**
     * Show toast message alongside notification
     */
    private fun showToastMessage(message: String) {
        // Ensure we're on the main thread for Toast
        Handler(Looper.getMainLooper()).post {
            if (canShowCustomToast()) {
                showCustomTopToast(message)
            } else {
                // Fallback to regular toast
                val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
                }
                toast.show()
            }
        }
    }
    
    /**
     * Check if we can show custom overlay toast
     */
    private fun canShowCustomToast(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Request overlay permission for custom toast
     * Call this from an Activity to request permission
     */
    fun requestOverlayPermission(activity: android.app.Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            activity.startActivity(intent)
        }
    }
    
    /**
     * Show custom toast at top of screen using overlay
     */
    private fun showCustomTopToast(message: String) {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Create toast view
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val toastView = TextView(context).apply {
                text = message
                setPadding(48, 24, 48, 24)
                setBackgroundResource(android.R.drawable.toast_frame)
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                gravity = Gravity.CENTER
            }
            
            // Window layout params for overlay
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100 // 100px from top
            }
            
            // Add view to window
            windowManager.addView(toastView, params)
            
            // Remove after 3.5 seconds (LENGTH_LONG duration)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    windowManager.removeView(toastView)
                } catch (e: Exception) {
                    // View might already be removed
                }
            }, 4000)
            
        } catch (e: Exception) {
            // Fallback to regular toast if custom toast fails
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Cancel all expense reminder notifications
     */
    fun cancelDailyExpenseReminder() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}