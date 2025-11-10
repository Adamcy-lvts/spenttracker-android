package com.example.spenttracker.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.spenttracker.data.sync.SyncManager
import com.example.spenttracker.data.sync.SyncStatusManager
import com.example.spenttracker.data.auth.UserContextProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Entry point interface for manual dependency injection
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun syncManager(): SyncManager
    fun userContextProvider(): UserContextProvider
    fun syncStatusManager(): SyncStatusManager
}

/**
 * Background Sync Worker - Handles periodic data synchronization
 * 
 * Like Laravel's queue jobs but for client-side background sync:
 * - Runs periodically to sync local changes to server
 * - Downloads server changes to local database
 * - Handles retry logic and failure scenarios
 * - Respects battery optimization and network conditions
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val userContextProvider: UserContextProvider,
    private val syncStatusManager: SyncStatusManager
) : CoroutineWorker(context, workerParams) {

    /**
     * Fallback constructor for when HiltWorkerFactory is not available
     * This uses EntryPoint to manually get dependencies
     */
    constructor(
        context: Context,
        workerParams: WorkerParameters
    ) : this(
        context = context,
        workerParams = workerParams,
        syncManager = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SyncWorkerEntryPoint::class.java
        ).syncManager(),
        userContextProvider = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SyncWorkerEntryPoint::class.java
        ).userContextProvider(),
        syncStatusManager = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SyncWorkerEntryPoint::class.java
        ).syncStatusManager()
    ) {
        Log.d(TAG, "Using fallback constructor - HiltWorkerFactory not available")
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "periodic_sync_work"
        const val SYNC_TYPE_KEY = "sync_type"
        const val SHOW_TOAST_KEY = "show_toast"
        const val OFFLINE_QUEUE_KEY = "offline_queue"
        
        // Sync types
        const val SYNC_TYPE_FULL = "full_sync"
        const val SYNC_TYPE_UPLOAD_ONLY = "upload_only"
        const val SYNC_TYPE_DOWNLOAD_ONLY = "download_only"
        
        /**
         * Schedule periodic background sync (like Laravel's scheduled jobs)
         */
        fun schedulePeriodicSync(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15, // Every 15 minutes
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5, // Allow 5 minutes flexibility
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(SYNC_TYPE_KEY to SYNC_TYPE_FULL))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10000, // 10 seconds minimum backoff
                    TimeUnit.MILLISECONDS
                )
                .addTag("sync")
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace existing work
                periodicSyncRequest
            )

            Log.i(TAG, "Periodic sync scheduled every 15 minutes")
        }
        
        /**
         * Trigger immediate one-time sync (like Laravel's dispatch now)
         */
        fun triggerImmediateSync(
            workManager: WorkManager,
            syncType: String = SYNC_TYPE_FULL
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(SYNC_TYPE_KEY to syncType))
                .addTag("sync_immediate")
                .build()

            workManager.enqueueUniqueWork(
                "immediate_sync_${System.currentTimeMillis()}",
                ExistingWorkPolicy.KEEP,
                immediateSyncRequest
            )

            Log.i(TAG, "Immediate sync triggered: $syncType")
        }
        
        /**
         * Cancel all sync work (like Laravel's cancel jobs)
         */
        fun cancelAllSync(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelAllWorkByTag("sync")
            workManager.cancelAllWorkByTag("sync_immediate")
            Log.i(TAG, "All sync work cancelled")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.i(TAG, "Starting background sync work")
            
            // Determine sync type from input data
            val syncType = inputData.getString(SYNC_TYPE_KEY) ?: SYNC_TYPE_FULL
            val shouldShowToast = inputData.getBoolean(SHOW_TOAST_KEY, false)
            val isOfflineQueued = inputData.getBoolean(OFFLINE_QUEUE_KEY, false)
            
            // Show sync started notification on main thread (only if not background delayed sync)
            if (!shouldShowToast) {
                withContext(Dispatchers.Main) {
                    syncStatusManager.showSyncStarted(syncType)
                }
            }
            
            // Check if user is logged in
            val userId = userContextProvider.getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "No user logged in - skipping sync")
                withContext(Dispatchers.Main) {
                    syncStatusManager.showSyncSkipped("No user logged in")
                }
                return@withContext Result.success()
            }
            
            Log.d(TAG, "Running sync for user: $userId")
            
            val syncResult = when (syncType) {
                SYNC_TYPE_FULL -> {
                    Log.d(TAG, "Performing full sync (upload + download)")
                    performFullSync(syncType)
                }
                SYNC_TYPE_UPLOAD_ONLY -> {
                    Log.d(TAG, "Performing upload-only sync")
                    performUploadSync(syncType)
                }
                SYNC_TYPE_DOWNLOAD_ONLY -> {
                    Log.d(TAG, "Performing download-only sync")
                    performDownloadSync(syncType)
                }
                else -> {
                    Log.w(TAG, "Unknown sync type: $syncType, defaulting to full sync")
                    performFullSync(SYNC_TYPE_FULL)
                }
            }
            
            // Show result notification on main thread
            withContext(Dispatchers.Main) {
                if (syncResult) {
                    Log.i(TAG, "Background sync completed successfully")
                    if (shouldShowToast) {
                        if (isOfflineQueued) {
                            syncStatusManager.showOfflineSyncSuccess()
                        } else {
                            syncStatusManager.showBackgroundSyncSuccess()
                        }
                    } else {
                        syncStatusManager.showSyncSuccess(syncType)
                    }
                } else {
                    Log.w(TAG, "Background sync completed with errors")
                    if (shouldShowToast) {
                        if (isOfflineQueued) {
                            syncStatusManager.showOfflineSyncFailure("Sync failed after network restored")
                        } else {
                            syncStatusManager.showBackgroundSyncFailure("Sync completed with errors")
                        }
                    } else {
                        syncStatusManager.showSyncFailure(syncType, "Sync completed with errors")
                    }
                }
            }
            
            if (syncResult) Result.success() else Result.retry()
            
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed with exception: ${e.message}", e)
            
            // Determine sync type for error notification
            val syncType = inputData.getString(SYNC_TYPE_KEY) ?: SYNC_TYPE_FULL
            
            // Show error notification on main thread
            withContext(Dispatchers.Main) {
                if (isRetriableError(e) && runAttemptCount < 3) {
                    syncStatusManager.showSyncRetry(syncType, runAttemptCount + 1)
                } else {
                    syncStatusManager.showSyncFailure(syncType, e.message)
                }
            }
            
            // Check if this is a network error that should be retried
            if (isRetriableError(e) && runAttemptCount < 3) {
                Log.d(TAG, "Error is retriable - scheduling retry")
                Result.retry()
            } else {
                Log.e(TAG, "Error is not retriable or max retries reached - marking as failure")
                Result.failure()
            }
        }
    }
    
    /**
     * Perform full synchronization (upload + download)
     */
    private suspend fun performFullSync(syncType: String): Boolean {
        return try {
            var success = true
            
            // Set up sync completion tracking
            var syncCompleted = false
            var syncSuccessful = false
            
            syncManager.setOnSyncSuccess {
                syncSuccessful = true
                syncCompleted = true
                Log.d(TAG, "Full sync completed successfully")
            }
            
            syncManager.setOnSyncError { error ->
                syncSuccessful = false
                syncCompleted = true
                Log.e(TAG, "Full sync failed: $error")
            }
            
            // Start the sync
            syncManager.startFullSync()
            
            // Wait for sync to complete (with timeout)
            val startTime = System.currentTimeMillis()
            val timeoutMs = 60000L // 1 minute timeout
            
            while (!syncCompleted && (System.currentTimeMillis() - startTime) < timeoutMs) {
                kotlinx.coroutines.delay(1000) // Check every second
            }
            
            if (!syncCompleted) {
                Log.e(TAG, "Full sync timed out")
                syncManager.cancelSync()
                false
            } else {
                syncSuccessful
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception during full sync: ${e.message}", e)
            false
        }
    }
    
    /**
     * Perform upload-only sync (for when we have local changes to push)
     */
    private suspend fun performUploadSync(syncType: String): Boolean {
        // For now, full sync includes upload, so we can use that
        // In the future, you could add upload-only methods to SyncManager
        return performFullSync(syncType)
    }
    
    /**
     * Perform download-only sync (for when we want to get server updates)
     */
    private suspend fun performDownloadSync(syncType: String): Boolean {
        // For now, full sync includes download, so we can use that
        // In the future, you could add download-only methods to SyncManager
        return performFullSync(syncType)
    }
    
    /**
     * Check if an error should trigger a retry
     */
    private fun isRetriableError(exception: Exception): Boolean {
        return when {
            // Network-related exceptions should be retried
            exception.message?.contains("network", ignoreCase = true) == true -> true
            exception.message?.contains("timeout", ignoreCase = true) == true -> true
            exception.message?.contains("connection", ignoreCase = true) == true -> true
            
            // HTTP errors in the 5xx range should be retried
            exception.message?.contains("5") == true -> true
            
            // Authentication errors (401) should not be retried immediately
            exception.message?.contains("401") == true -> false
            exception.message?.contains("unauthorized", ignoreCase = true) == true -> false
            
            // Default to retry for unknown errors
            else -> true
        }
    }
}