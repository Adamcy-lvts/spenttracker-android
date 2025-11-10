package com.example.spenttracker.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberLocationPermissionHandler(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): LocationPermissionHandler {
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    return remember {
        LocationPermissionHandler(
            context = context,
            permissionLauncher = permissionLauncher,
            onPermissionGranted = onPermissionGranted,
            onPermissionDenied = onPermissionDenied
        )
    }
}

class LocationPermissionHandler(
    private val context: Context,
    private val permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit
) {
    
    private var showRationale by mutableStateOf(false)
    private var showSettingsDialog by mutableStateOf(false)
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestLocationPermission() {
        if (hasLocationPermission()) {
            onPermissionGranted()
            return
        }
        
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    fun showRationaleDialog() {
        showRationale = true
    }
    
    fun showSettingsDialog() {
        showSettingsDialog = true
    }
    
    @Composable
    fun LocationPermissionDialogs() {
        // Rationale dialog
        if (showRationale) {
            AlertDialog(
                onDismissRequest = { 
                    showRationale = false
                    onPermissionDenied()
                },
                title = { Text("Location Permission Required") },
                text = { 
                    Text(
                        "SpentTracker uses your location to track where you log in for security purposes. " +
                        "This helps protect your account by monitoring unusual login locations."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRationale = false
                            requestLocationPermission()
                        }
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRationale = false
                            onPermissionDenied()
                        }
                    ) {
                        Text("Skip")
                    }
                }
            )
        }
        
        // Settings dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showSettingsDialog = false
                    onPermissionDenied()
                },
                title = { Text("Enable Location in Settings") },
                text = { 
                    Text(
                        "Location permission was denied. You can enable it in the app settings " +
                        "to allow SpentTracker to track your login location for security purposes."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            openAppSettings()
                        }
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSettingsDialog = false
                            onPermissionDenied()
                        }
                    ) {
                        Text("Skip")
                    }
                }
            )
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}