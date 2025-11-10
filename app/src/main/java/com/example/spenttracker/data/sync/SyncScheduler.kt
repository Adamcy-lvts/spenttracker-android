package com.example.spenttracker.data.sync

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.example.spenttracker.data.worker.SyncWorker
import com.example.spenttracker.data.auth.UserContextProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Scheduler - Manages when and how sync operations are triggered
 * 
 * Like Laravel's job scheduler but for client-side sync operations:
 * - Schedules periodic background sync
 * - Triggers immediate sync on user actions
 * - Manages sync frequency based on user activity
 * - Handles sync lifecycle during login/logout
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userContextProvider: UserContextProvider,
    private val syncStatusManager: SyncStatusManager
) {
    
    companion object {
        private const val TAG = "SyncScheduler"
        private const val SYNC_WORK_NAME = "expense_sync"
        private const val PERIODIC_SYNC_NAME = "periodic_expense_sync"
        private const val DELAYED_SYNC_NAME = "delayed_expense_sync"
        private const val SYNC_TAG = "sync_tag"
        private const val DELAYED_SYNC_DELAY_MINUTES = 5L // Production: 5 minutes delay after CRUD operations
    }
    
    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }
    
    /**
     * Start sync scheduling for logged-in user
     * Call this after successful login
     */
    fun startSyncScheduling() {
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            Log.i(TAG, "Starting sync scheduling for user: $userId")
            
            // Schedule periodic background sync
            schedulePeriodicSync()
            
            // Trigger immediate sync to get latest data
            scheduleImmediateSync()
            
        } else {
            Log.w(TAG, "Cannot start sync scheduling - no user logged in")
        }
    }
    
    /**
     * Stop sync scheduling (call during logout)
     */
    fun stopSyncScheduling() {
        Log.i(TAG, "Stopping sync scheduling")
        cancelAllSync()
    }
    
    /**
     * Schedule a one-time sync immediately
     */
    fun scheduleImmediateSync() {
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            Log.d(TAG, "Scheduling immediate sync for user: $userId")
            
            // Cancel any existing immediate sync first
            workManager.cancelAllWorkByTag(SYNC_TAG)
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            
            val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(SYNC_TAG)
                .setInputData(androidx.work.workDataOf(SyncWorker.SYNC_TYPE_KEY to SyncWorker.SYNC_TYPE_FULL))
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            workManager.enqueueUniqueWork(
                SYNC_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            
            // Show manual sync notification
            syncStatusManager.showManualSyncTriggered()
        } else {
            Log.w(TAG, "Cannot trigger sync - no user logged in")
            syncStatusManager.showSyncSkipped("Please log in to sync data")
        }
    }
    
    /**
     * Schedule periodic sync every 15 minutes
     */
    fun schedulePeriodicSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val periodicSyncRequest = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES,
            5, java.util.concurrent.TimeUnit.MINUTES // flex period
        )
            .setConstraints(constraints)
            .addTag(SYNC_TAG)
            .setInputData(androidx.work.workDataOf(SyncWorker.SYNC_TYPE_KEY to SyncWorker.SYNC_TYPE_FULL))
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            periodicSyncRequest
        )
        
        Log.i(TAG, "Periodic sync scheduled every 15 minutes")
    }
    
    /**
     * Cancel all sync work
     */
    fun cancelAllSync() {
        workManager.cancelAllWorkByTag(SYNC_TAG)
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_SYNC_NAME)
        Log.i(TAG, "All sync work cancelled")
    }
    
    /**
     * Trigger immediate sync (call after user creates/edits expense)
     */
    fun triggerImmediateSync() {
        scheduleImmediateSync()
    }
    
    /**
     * Schedule delayed sync after CRUD operations (1 minute delay for testing, 5 minutes for production)
     */
    fun scheduleDelayedSync() {
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            Log.d(TAG, "Scheduling delayed sync for user: $userId in $DELAYED_SYNC_DELAY_MINUTES minute(s)")
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false) // Allow sync even on low battery for important data
                .build()
            
            val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(SYNC_TAG)
                .setInputData(androidx.work.workDataOf(
                    SyncWorker.SYNC_TYPE_KEY to SyncWorker.SYNC_TYPE_UPLOAD_ONLY,
                    SyncWorker.SHOW_TOAST_KEY to true // Show toast notification after sync
                ))
                .setInitialDelay(DELAYED_SYNC_DELAY_MINUTES, java.util.concurrent.TimeUnit.MINUTES)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            workManager.enqueueUniqueWork(
                DELAYED_SYNC_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE, // Replace any existing delayed sync
                syncWorkRequest
            )
            
            Log.i(TAG, "Delayed sync scheduled for ${DELAYED_SYNC_DELAY_MINUTES} minute(s)")
        } else {
            Log.w(TAG, "Cannot schedule delayed sync - no user logged in")
        }
    }
    
    /**
     * Trigger upload-only sync (for backward compatibility)
     */
    fun triggerUploadSync() {
        scheduleDelayedSync()
    }
    
    /**
     * Schedule sync for when network becomes available
     * This will queue the sync to run immediately when internet connection is restored
     */
    fun scheduleOfflineSync() {
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            Log.d(TAG, "Scheduling offline sync queue for user: $userId - will run when internet returns")
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED) // Will wait for network
                .setRequiresBatteryNotLow(false)
                .build()
            
            val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(SYNC_TAG)
                .setInputData(androidx.work.workDataOf(
                    SyncWorker.SYNC_TYPE_KEY to SyncWorker.SYNC_TYPE_UPLOAD_ONLY,
                    SyncWorker.SHOW_TOAST_KEY to true,
                    SyncWorker.OFFLINE_QUEUE_KEY to true // Mark as offline queued sync
                ))
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            workManager.enqueueUniqueWork(
                "offline_sync_queue",
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            
            Log.i(TAG, "Offline sync queued - will execute when internet connection is restored")
        } else {
            Log.w(TAG, "Cannot queue offline sync - no user logged in")
        }
    }
    
    /**
     * Trigger download-only sync (call when user wants to refresh data)
     */
    fun triggerDownloadSync() {
        val userId = userContextProvider.getCurrentUserId()
        if (userId != null) {
            Log.d(TAG, "Triggering download sync for user: $userId")
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            
            val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag(SYNC_TAG)
                .setInputData(androidx.work.workDataOf(SyncWorker.SYNC_TYPE_KEY to SyncWorker.SYNC_TYPE_DOWNLOAD_ONLY))
                .build()
            
            workManager.enqueueUniqueWork(
                "download_sync_${System.currentTimeMillis()}",
                androidx.work.ExistingWorkPolicy.KEEP,
                syncWorkRequest
            )
            
            syncStatusManager.showManualSyncTriggered()
        } else {
            Log.w(TAG, "Cannot trigger download sync - no user logged in")
            syncStatusManager.showSyncSkipped("Please log in to sync data")
        }
    }
    
    /**
     * Check if sync is currently scheduled
     */
    fun isSyncScheduled(): Boolean {
        return try {
            // Check if periodic work is enqueued
            val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get()
            workInfos.any { workInfo -> 
                !workInfo.state.isFinished
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking sync schedule: ${e.message}")
            false
        }
    }
    
    /**
     * Get sync status information
     */
    fun getSyncStatus(): SyncScheduleStatus {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(SyncWorker.WORK_NAME).get()
            val isScheduled = workInfos.any { !it.state.isFinished }
            
            val lastRunTime = workInfos.maxOfOrNull { workInfo ->
                workInfo.outputData.getLong("completion_time", 0L)
            } ?: 0L
            
            SyncScheduleStatus(
                isScheduled = isScheduled,
                lastSyncTime = if (lastRunTime > 0) lastRunTime else null,
                userId = userContextProvider.getCurrentUserId()
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Error getting sync status: ${e.message}")
            SyncScheduleStatus(
                isScheduled = false,
                lastSyncTime = null,
                userId = userContextProvider.getCurrentUserId()
            )
        }
    }
}

/**
 * Sync schedule status data class
 */
data class SyncScheduleStatus(
    val isScheduled: Boolean,
    val lastSyncTime: Long? = null,
    val userId: Long? = null
) {
    val isUserLoggedIn: Boolean
        get() = userId != null
        
    val lastSyncTimeFormatted: String
        get() = if (lastSyncTime != null) {
            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(lastSyncTime))
        } else {
            "Never"
        }
}