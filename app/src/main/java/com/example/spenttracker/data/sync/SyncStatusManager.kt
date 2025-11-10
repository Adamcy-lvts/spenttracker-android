package com.example.spenttracker.data.sync

import android.content.Context
import android.widget.Toast
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Status Manager - Handles UI feedback for sync operations
 * 
 * Like Laravel's notification system but for sync status updates:
 * - Shows toast notifications for sync success/failure
 * - Manages sync status messages for different scenarios
 * - Provides consistent UI feedback across the app
 */
@Singleton
class SyncStatusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SyncStatusManager"
    }
    
    /**
     * Show sync started notification
     */
    fun showSyncStarted(syncType: String = "full") {
        val message = when (syncType) {
            "full_sync" -> "Syncing data..."
            "upload_only" -> "Uploading changes..."
            "download_only" -> "Checking for updates..."
            else -> "Syncing..."
        }
        
        Log.d(TAG, "Sync started: $message")
        showToast(message, Toast.LENGTH_SHORT)
    }
    
    /**
     * Show sync success notification
     */
    fun showSyncSuccess(syncType: String = "full") {
        val message = when (syncType) {
            "full_sync" -> "Sync completed successfully"
            "upload_only" -> "Changes uploaded successfully"
            "download_only" -> "Data updated successfully"
            else -> "Sync successful"
        }
        
        Log.i(TAG, "Sync success: $message")
        showToast(message, Toast.LENGTH_SHORT)
    }
    
    /**
     * Show sync failure notification
     */
    fun showSyncFailure(syncType: String = "full", error: String? = null) {
        val baseMessage = when (syncType) {
            "full_sync" -> "Sync failed"
            "upload_only" -> "Failed to upload changes"
            "download_only" -> "Failed to get updates"
            else -> "Sync failed"
        }
        
        val message = if (error != null && isNetworkError(error)) {
            "$baseMessage - Check internet connection"
        } else {
            baseMessage
        }
        
        Log.w(TAG, "Sync failure: $message (error: $error)")
        showToast(message, Toast.LENGTH_LONG)
    }
    
    /**
     * Show sync retry notification
     */
    fun showSyncRetry(syncType: String = "full", attemptNumber: Int) {
        val message = "Sync failed, retrying... (attempt $attemptNumber)"
        
        Log.d(TAG, "Sync retry: $message")
        showToast(message, Toast.LENGTH_SHORT)
    }
    
    /**
     * Show when sync is skipped (no user logged in)
     */
    fun showSyncSkipped(reason: String = "No user logged in") {
        Log.d(TAG, "Sync skipped: $reason")
        // Don't show toast for this - it's not user-actionable
    }
    
    /**
     * Show manual sync triggered notification
     */
    fun showManualSyncTriggered() {
        Log.d(TAG, "Manual sync triggered")
        showToast("Refreshing data...", Toast.LENGTH_SHORT)
    }
    
    /**
     * Show background sync notification (subtle)
     */
    fun showBackgroundSyncCompleted() {
        Log.d(TAG, "Background sync completed")
        // For background sync, only show if there were updates
        // We could enhance this to show "New data available" if changes were downloaded
    }
    
    /**
     * Show background sync success toast
     */
    fun showBackgroundSyncSuccess() {
        Log.i(TAG, "Background sync completed successfully")
        showToast("✓ Data synced successfully", Toast.LENGTH_SHORT)
    }
    
    /**
     * Show background sync failure toast
     */
    fun showBackgroundSyncFailure(error: String? = null) {
        val message = if (error != null && isNetworkError(error)) {
            "✗ Sync failed - Check internet connection"
        } else {
            "✗ Sync failed - Will retry later"
        }
        
        Log.w(TAG, "Background sync failed: $message (error: $error)")
        showToast(message, Toast.LENGTH_LONG)
    }
    
    /**
     * Show offline sync success toast (when internet returns)
     */
    fun showOfflineSyncSuccess() {
        Log.i(TAG, "Offline sync completed successfully after network restored")
        showToast("🌐 Internet restored - Data synced successfully", Toast.LENGTH_SHORT)
    }
    
    /**
     * Show offline sync failure toast (when internet returns but sync still fails)
     */
    fun showOfflineSyncFailure(error: String? = null) {
        val message = "🌐 Internet restored but sync failed - Will retry later"
        
        Log.w(TAG, "Offline sync failed after network restored: $message (error: $error)")
        showToast(message, Toast.LENGTH_LONG)
    }
    
    /**
     * Check if error is network-related
     */
    private fun isNetworkError(error: String): Boolean {
        return error.contains("network", ignoreCase = true) ||
               error.contains("connection", ignoreCase = true) ||
               error.contains("timeout", ignoreCase = true) ||
               error.contains("unreachable", ignoreCase = true)
    }
    
    /**
     * Show toast on main thread
     */
    private fun showToast(message: String, duration: Int) {
        // Ensure we're on main thread for Toast
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            Toast.makeText(context, message, duration).show()
        } else {
            // Post to main thread
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, message, duration).show()
            }
        }
    }
}