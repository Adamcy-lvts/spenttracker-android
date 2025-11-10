package com.example.spenttracker

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for SpentTracker
 * Handles app-wide initialization - Like Laravel's AppServiceProvider
 */
@HiltAndroidApp
class SpentTrackerApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    /**
     * App initialization - Like Laravel's boot() method
     */
    override fun onCreate() {
        super.onCreate()
        
        // STEP 1: Initialize Google Places API
        initializePlacesApi()
        
        // STEP 2: Clear old WorkManager database if needed
        clearOldWorkManagerDatabase()
        
        // STEP 3: Initialize WorkManager with HiltWorkerFactory
        initializeWorkManager()
    }
    
    private fun initializePlacesApi() {
        try {
            // Initialize Places API - it will automatically read API key from AndroidManifest.xml
            if (!Places.isInitialized()) {
                // Read the API key from AndroidManifest.xml meta-data
                val apiKey = packageManager.getApplicationInfo(packageName, 
                    android.content.pm.PackageManager.GET_META_DATA)
                    .metaData?.getString("com.google.android.geo.API_KEY")
                
                if (!apiKey.isNullOrBlank()) {
                    Places.initialize(applicationContext, apiKey)
                    Log.d("SpentTrackerApp", "✓ Google Places API initialized with key")
                } else {
                    Log.w("SpentTrackerApp", "No Google API key found in AndroidManifest.xml")
                    Log.w("SpentTrackerApp", "Add <meta-data android:name=\"com.google.android.geo.API_KEY\" android:value=\"YOUR_KEY\" />")
                }
            } else {
                Log.d("SpentTrackerApp", "✓ Google Places API already initialized")
            }
        } catch (e: Exception) {
            Log.e("SpentTrackerApp", "Failed to initialize Places API: ${e.message}", e)
            Log.w("SpentTrackerApp", "Location features will work with geocoding only (no enhanced place names)")
        }
    }
    
    private fun clearOldWorkManagerDatabase() {
        try {
            // Check if this is first run with new configuration
            val prefs = getSharedPreferences("app_config", MODE_PRIVATE)
            val hasCleared = prefs.getBoolean("workmanager_cleared_v2", false)
            
            if (!hasCleared) {
                Log.d("SpentTrackerApp", "Clearing old WorkManager database...")
                
                val databaseDir = java.io.File(applicationInfo.dataDir, "databases")
                if (databaseDir.exists()) {
                    databaseDir.listFiles()?.forEach { file ->
                        if (file.name.contains("androidx.work")) {
                            file.delete()
                            Log.d("SpentTrackerApp", "Deleted: ${file.name}")
                        }
                    }
                }
                
                prefs.edit().putBoolean("workmanager_cleared_v2", true).apply()
                Log.d("SpentTrackerApp", "WorkManager database cleared")
            }
        } catch (e: Exception) {
            Log.e("SpentTrackerApp", "Error clearing WorkManager database", e)
        }
    }
    
    private fun initializeWorkManager() {
        try {
            val config = Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
            
            WorkManager.initialize(this, config)
            Log.d("SpentTrackerApp", "✓ WorkManager initialized with HiltWorkerFactory")
            
            // Verify
            val instance = WorkManager.getInstance(this)
            Log.d("SpentTrackerApp", "✓ WorkManager instance verified: ${instance != null}")
            
        } catch (e: IllegalStateException) {
            // WorkManager might already be initialized
            Log.w("SpentTrackerApp", "WorkManager already initialized", e)
        } catch (e: Exception) {
            Log.e("SpentTrackerApp", "Failed to initialize WorkManager", e)
        }
    }
    
    /**
     * Provide WorkManager configuration with HiltWorkerFactory
     * This is called by WorkManager to get configuration
     */
    override val workManagerConfiguration: Configuration
        get() {
            Log.d("SpentTrackerApp", "workManagerConfiguration called")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        }
}