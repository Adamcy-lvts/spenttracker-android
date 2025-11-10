package com.example.spenttracker.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.edit

/**
 * Sequential Permission Manager
 * Handles multiple permissions one by one for better UX
 */
class SequentialPermissionManager(
    private val activity: ComponentActivity,
    private val onAllPermissionsHandled: () -> Unit = {}
) {
    private var currentStep = 0
    private val steps = mutableListOf<PermissionStep>()
    
    // Permission launchers
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.d("PermissionManager", "Notification permission granted")
        } else {
            android.util.Log.w("PermissionManager", "Notification permission denied")
        }
        proceedToNextStep()
    }
    
    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            android.util.Log.d("PermissionManager", "Location permissions granted")
        } else {
            android.util.Log.w("PermissionManager", "Some location permissions denied")
        }
        proceedToNextStep()
    }
    
    private fun proceedToNextStep() {
        currentStep++
        if (currentStep < steps.size) {
            executeCurrentStep()
        } else {
            // All permissions handled
            onAllPermissionsHandled()
        }
    }
    
    private fun executeCurrentStep() {
        if (currentStep < steps.size) {
            val step = steps[currentStep]
            step.execute()
        }
    }
    
    fun startPermissionFlow() {
        currentStep = 0
        steps.clear()
        
        // Add notification permission step (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                steps.add(NotificationPermissionStep())
            }
        }
        
        // Add location permissions step
        val hasLocationCoarse = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocationFine = ActivityCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocationCoarse || !hasLocationFine) {
            steps.add(LocationPermissionStep())
        }
        
        // Add overlay permission step
        val prefs = activity.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val hasShownOverlayDialog = prefs.getBoolean("has_shown_overlay_dialog", false)
        if (!hasShownOverlayDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                steps.add(OverlayPermissionStep())
            }
        }
        
        // Start the flow
        if (steps.isNotEmpty()) {
            executeCurrentStep()
        } else {
            onAllPermissionsHandled()
        }
    }
    
    private sealed class PermissionStep {
        abstract fun execute()
    }
    
    private inner class NotificationPermissionStep : PermissionStep() {
        override fun execute() {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    private inner class LocationPermissionStep : PermissionStep() {
        override fun execute() {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }
    
    private inner class OverlayPermissionStep : PermissionStep() {
        override fun execute() {
            // This will be handled by the compose dialog
            // Mark as shown and proceed
            val prefs = activity.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            prefs.edit {
                putBoolean("has_shown_overlay_dialog", true)
            }
            proceedToNextStep()
        }
    }
}

/**
 * Composable hook for sequential permissions
 */
@Composable
fun rememberSequentialPermissionManager(
    onAllPermissionsHandled: () -> Unit = {}
): SequentialPermissionManager {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    
    return remember {
        SequentialPermissionManager(activity, onAllPermissionsHandled)
    }
}