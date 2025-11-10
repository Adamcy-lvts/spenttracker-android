package com.example.spenttracker.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerMigration @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WorkManagerMigration"
        private const val PREF_NAME = "workmanager_migration"
        private const val KEY_MIGRATED = "database_migrated"
    }
    
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if we need to clear WorkManager database
     */
    fun needsMigration(): Boolean {
        return !prefs.getBoolean(KEY_MIGRATED, false)
    }
    
    /**
     * Clear WorkManager database files
     * Call this BEFORE WorkManager.initialize()
     */
    fun clearWorkManagerDatabase() {
        if (!needsMigration()) {
            Log.d(TAG, "Migration already completed")
            return
        }
        
        try {
            Log.d(TAG, "Starting WorkManager database cleanup...")
            
            // Get database directory
            val databaseDir = File(context.applicationInfo.dataDir, "databases")
            if (databaseDir.exists() && databaseDir.isDirectory) {
                // Find and delete WorkManager database files
                databaseDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("androidx.work.workdb") ||
                        file.name.startsWith("androidx.work")) {
                        val deleted = file.delete()
                        Log.d(TAG, "Deleted ${file.name}: $deleted")
                    }
                }
            }
            
            // Mark migration as complete
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
            Log.d(TAG, "WorkManager database cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear WorkManager database", e)
        }
    }
    
    /**
     * Reset migration flag (for testing purposes)
     */
    fun resetMigration() {
        prefs.edit().putBoolean(KEY_MIGRATED, false).apply()
    }
}