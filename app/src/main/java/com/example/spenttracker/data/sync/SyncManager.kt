package com.example.spenttracker.data.sync

import android.util.Log
import com.example.spenttracker.data.local.ExpenseDao
import com.example.spenttracker.data.local.CategoryDao
import com.example.spenttracker.data.local.entity.ExpenseEntity
import com.example.spenttracker.data.local.entity.CategoryEntity
import com.example.spenttracker.data.local.entity.SyncStatus
import com.example.spenttracker.data.local.entity.CategorySyncStatus
import com.example.spenttracker.data.remote.api.ExpenseApiService
import com.example.spenttracker.data.remote.api.CategoryApiService
import com.example.spenttracker.data.mapper.toDomain
import com.example.spenttracker.data.mapper.toDomainList
import com.example.spenttracker.data.mapper.toEntity
import com.example.spenttracker.data.mapper.ExpenseDtoMapper
import com.example.spenttracker.data.mapper.CategoryDtoMapper
import com.example.spenttracker.data.auth.UserContextProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Manager - Orchestrates data synchronization between local DB and Laravel API
 * 
 * How it works:
 * 1. **Upload Sync**: Send local changes to server
 * 2. **Download Sync**: Get server changes to local
 * 3. **Conflict Resolution**: Handle data conflicts intelligently  
 * 4. **Background Sync**: Automatic periodic synchronization
 * 
 * Like Laravel's queue system but for client-server data sync
 */
@Singleton
class SyncManager @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val expenseApiService: ExpenseApiService,
    private val categoryApiService: CategoryApiService,
    private val userContextProvider: UserContextProvider,
    private val expenseDtoMapper: ExpenseDtoMapper,
    private val categoryDtoMapper: CategoryDtoMapper
) {
    
    companion object {
        private const val TAG = "SyncManager"
        private const val MAX_SYNC_RETRIES = 3
        private const val SYNC_BATCH_SIZE = 20
    }
    
    private var syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    
    // Sync state
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // Sync callbacks
    private var onSyncSuccess: (() -> Unit)? = null
    private var onSyncError: ((String) -> Unit)? = null
    
    /**
     * Start full synchronization (upload + download)
     */
    fun startFullSync() {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "Sync already in progress - skipping")
            return
        }
        
        val userId = userContextProvider.getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "No user context - cannot sync")
            return
        }
        
        syncJob = syncScope.launch {
            try {
                _syncState.value = SyncState.Syncing
                Log.i(TAG, "Starting full sync for user: $userId")
                
                // Step 1: Sync categories first (they're needed for expenses)
                val categoryUploadSuccess = uploadPendingCategories()
                val categoryDownloadSuccess = downloadServerCategories()
                
                if (categoryUploadSuccess && categoryDownloadSuccess) {
                    // Step 2: Upload local expense changes to server
                    val expenseUploadSuccess = uploadPendingChanges(userId)
                    
                    if (expenseUploadSuccess) {
                        // Step 3: Download server expense changes to local
                        val expenseDownloadSuccess = downloadServerChanges(userId)
                        
                        if (expenseDownloadSuccess) {
                            _syncState.value = SyncState.Success
                            Log.i(TAG, "Full sync completed successfully")
                            
                            withContext(Dispatchers.Main) {
                                onSyncSuccess?.invoke()
                            }
                        } else {
                            throw Exception("Expense download sync failed")
                        }
                    } else {
                        throw Exception("Expense upload sync failed")
                    }
                } else {
                    throw Exception("Category sync failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}", e)
                _syncState.value = SyncState.Error(e.message ?: "Unknown sync error")
                
                withContext(Dispatchers.Main) {
                    onSyncError?.invoke(e.message ?: "Sync failed")
                }
            }
        }
    }
    
    /**
     * Upload local changes to server (CREATE, UPDATE, DELETE)
     */
    private suspend fun uploadPendingChanges(userId: Long): Boolean {
        Log.d(TAG, "Starting upload sync for user: $userId")
        
        try {
            // Get all expenses that need syncing
            val pendingExpenses = expenseDao.getExpensesNeedingSync(userId)
            
            if (pendingExpenses.isEmpty()) {
                Log.d(TAG, "No pending changes to upload")
                return true
            }
            
            Log.i(TAG, "Found ${pendingExpenses.size} expenses needing sync")
            
            // Process in batches to avoid overwhelming the server
            val batches = pendingExpenses.chunked(SYNC_BATCH_SIZE)
            
            for ((batchIndex, batch) in batches.withIndex()) {
                Log.d(TAG, "Processing upload batch ${batchIndex + 1}/${batches.size}")
                
                for (expense in batch) {
                    val success = when {
                        expense.syncStatus == SyncStatus.DELETED.name -> {
                            // Deleted expense - DELETE on server
                            uploadDeletedExpense(expense)
                        }
                        expense.serverId == null -> {
                            // New expense - CREATE on server
                            uploadNewExpense(expense)
                        }
                        expense.syncStatus == SyncStatus.PENDING.name -> {
                            // Modified expense - UPDATE on server
                            uploadUpdatedExpense(expense)
                        }
                        else -> true
                    }
                    
                    if (!success) {
                        Log.w(TAG, "Failed to upload expense: ${expense.id}")
                        // Continue with other expenses rather than failing entire sync
                    }
                }
            }
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Upload sync failed: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Upload a new expense to server
     */
    private suspend fun uploadNewExpense(expense: ExpenseEntity): Boolean {
        return try {
            // Mark as syncing
            expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCING.name)
            
            Log.d(TAG, "Uploading new expense: ${expense.description}")
            
            // Convert to API format and send
            val createRequest = expenseDtoMapper.toCreateRequest(expense.toDomain())
            val response = expenseApiService.createExpense(createRequest)
            
            if (response.isSuccessful) {
                val apiExpense = response.body()?.data
                if (apiExpense != null) {
                    // Update local record with server ID and mark as synced
                    expenseDao.updateServerInfo(
                        localId = expense.id,
                        serverId = apiExpense.id,
                        syncStatus = SyncStatus.SYNCED.name,
                        lastSyncAt = Instant.now().toString(),
                        needsSync = false
                    )
                    
                    Log.d(TAG, "Successfully uploaded expense: ${expense.description} -> server ID: ${apiExpense.id}")
                    true
                } else {
                    Log.w(TAG, "Upload successful but no response body")
                    expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
                    false
                }
            } else {
                Log.w(TAG, "Upload failed with code: ${response.code()}")
                expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading expense: ${e.message}", e)
            expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
            false
        }
    }
    
    /**
     * Upload an updated expense to server
     */
    private suspend fun uploadUpdatedExpense(expense: ExpenseEntity): Boolean {
        return try {
            if (expense.serverId == null) {
                Log.w(TAG, "Cannot update expense without server ID: ${expense.id}")
                return false
            }
            
            // Mark as syncing
            expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCING.name)
            
            Log.d(TAG, "Uploading updated expense: ${expense.description}")
            
            // Convert to API format and send update
            val updateRequest = expenseDtoMapper.toUpdateRequest(expense.toDomain())
            val response = expenseApiService.updateExpense(expense.serverId, updateRequest)
            
            if (response.isSuccessful) {
                // Mark as successfully synced
                expenseDao.updateServerInfo(
                    localId = expense.id,
                    serverId = expense.serverId,
                    syncStatus = SyncStatus.SYNCED.name,
                    lastSyncAt = Instant.now().toString(),
                    needsSync = false
                )
                
                Log.d(TAG, "Successfully updated expense: ${expense.description}")
                true
            } else {
                Log.w(TAG, "Update failed with code: ${response.code()}")
                expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating expense: ${e.message}", e)
            expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
            false
        }
    }
    
    /**
     * Delete expense from server
     */
    private suspend fun uploadDeletedExpense(expense: ExpenseEntity): Boolean {
        return try {
            if (expense.serverId == null) {
                Log.w(TAG, "Cannot delete expense without server ID: ${expense.id}")
                // Physically delete local record since it was never on server
                expenseDao.deleteExpenseById(expense.id)
                return true
            }
            
            // Mark as syncing
            expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCING.name)
            
            Log.d(TAG, "Deleting expense from server: ${expense.description}")
            
            // Send delete request to server
            val response = expenseApiService.deleteExpense(expense.serverId)
            
            if (response.isSuccessful) {
                // Successfully deleted on server - physically remove from local DB
                expenseDao.deleteExpenseById(expense.id)
                
                Log.d(TAG, "Successfully deleted expense: ${expense.description}")
                true
            } else if (response.code() == 404) {
                // Expense doesn't exist on server (already deleted or never existed)
                // Treat as successful deletion - physically remove from local DB
                expenseDao.deleteExpenseById(expense.id)
                
                Log.d(TAG, "Expense not found on server (already deleted): ${expense.description}")
                true
            } else {
                Log.w(TAG, "Delete failed with code: ${response.code()}")
                expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting expense: ${e.message}", e)
            expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED.name)
            false
        }
    }
    
    /**
     * Upload pending categories to server
     */
    private suspend fun uploadPendingCategories(): Boolean {
        Log.d(TAG, "Starting category upload sync")
        
        try {
            // Get all categories that need syncing
            val pendingCategories = categoryDao.getCategoriesNeedingSync()
            
            if (pendingCategories.isEmpty()) {
                Log.d(TAG, "No pending category changes to upload")
                return true
            }
            
            Log.i(TAG, "Found ${pendingCategories.size} categories needing sync")
            
            for (category in pendingCategories) {
                val success = when {
                    category.syncStatus == CategorySyncStatus.DELETED.name -> {
                        // Deleted category - DELETE on server
                        uploadDeletedCategory(category)
                    }
                    category.id < 0 -> {
                        // New category with temporary negative ID - CREATE on server
                        uploadNewCategory(category)
                    }
                    category.syncStatus == CategorySyncStatus.PENDING.name -> {
                        // Modified category - UPDATE on server
                        uploadUpdatedCategory(category)
                    }
                    else -> true
                }
                
                if (!success) {
                    Log.w(TAG, "Failed to upload category: ${category.id}")
                    // Continue with other categories rather than failing entire sync
                }
            }
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Category upload sync failed: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Upload a new category to server
     */
    private suspend fun uploadNewCategory(category: CategoryEntity): Boolean {
        return try {
            // Mark as syncing
            categoryDao.updateSyncStatus(category.id, CategorySyncStatus.SYNCING.name)
            
            Log.d(TAG, "Uploading new category: ${category.name}")
            
            // Convert to API format and send
            val createRequest = categoryDtoMapper.toCreateRequest(category.toDomain())
            val response = categoryApiService.createCategory(createRequest)
            
            if (response.isSuccessful) {
                val apiCategory = response.body()?.data
                if (apiCategory != null) {
                    val oldId = category.id // Temporary negative ID
                    val newId = apiCategory.id // Server ID
                    
                    // Replace local record: delete old, insert new with server ID
                    categoryDao.deleteCategoryById(oldId)
                    
                    // Create new record with server ID
                    val newEntity = categoryDtoMapper.toEntity(apiCategory)
                    categoryDao.insertCategory(newEntity)
                    
                    // Update any expenses that reference the old negative ID
                    val userId = userContextProvider.getCurrentUserId()
                    if (userId != null) {
                        expenseDao.updateExpensesCategoryId(
                            oldCategoryId = oldId,
                            newCategoryId = newId,
                            userId = userId
                        )
                    }
                    
                    Log.d(TAG, "Successfully uploaded category: ${category.name} (ID: $oldId -> $newId)")
                    true
                } else {
                    Log.w(TAG, "Category upload successful but no response body")
                    categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
                    false
                }
            } else {
                Log.w(TAG, "Category upload failed with code: ${response.code()}")
                categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading category: ${e.message}", e)
            categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
            false
        }
    }
    
    /**
     * Upload an updated category to server
     */
    private suspend fun uploadUpdatedCategory(category: CategoryEntity): Boolean {
        return try {
            if (category.id < 0) {
                Log.w(TAG, "Cannot update category with temporary negative ID: ${category.id}")
                return false
            }
            
            // Mark as syncing
            categoryDao.updateSyncStatus(category.id, CategorySyncStatus.SYNCING.name)
            
            Log.d(TAG, "Uploading updated category: ${category.name}")
            
            // Convert to API format and send update
            val updateRequest = categoryDtoMapper.toUpdateRequest(category.toDomain())
            val response = categoryApiService.updateCategory(category.id, updateRequest)
            
            if (response.isSuccessful) {
                // Mark as successfully synced  
                val updatedCategory = category.copy(
                    syncStatus = CategorySyncStatus.SYNCED.name,
                    needsSync = false,
                    lastSyncAt = Instant.now().toString(),
                    updatedAt = System.currentTimeMillis()
                )
                categoryDao.updateCategory(updatedCategory)
                
                Log.d(TAG, "Successfully updated category: ${category.name}")
                true
            } else {
                Log.w(TAG, "Category update failed with code: ${response.code()}")
                categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating category: ${e.message}", e)
            categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
            false
        }
    }
    
    /**
     * Delete category from server
     */
    private suspend fun uploadDeletedCategory(category: CategoryEntity): Boolean {
        return try {
            if (category.id < 0) {
                Log.w(TAG, "Deleting local-only category (never synced): ${category.id}")
                // Physically delete local record since it was never on server
                categoryDao.deleteCategoryById(category.id)
                return true
            }
            
            // Mark as syncing
            categoryDao.updateSyncStatus(category.id, CategorySyncStatus.SYNCING.name)
            
            Log.d(TAG, "Deleting category from server: ${category.name}")
            
            // Send delete request to server
            val response = categoryApiService.deleteCategory(category.id)
            
            if (response.isSuccessful) {
                // Successfully deleted on server - physically remove from local DB
                categoryDao.deleteCategoryById(category.id)
                
                Log.d(TAG, "Successfully deleted category: ${category.name}")
                true
            } else if (response.code() == 404) {
                // Category doesn't exist on server (already deleted or never existed)
                // Treat as successful deletion - physically remove from local DB
                categoryDao.deleteCategoryById(category.id)
                
                Log.d(TAG, "Category not found on server (already deleted): ${category.name}")
                true
            } else {
                Log.w(TAG, "Category delete failed with code: ${response.code()}")
                categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting category: ${e.message}", e)
            categoryDao.updateSyncStatus(category.id, CategorySyncStatus.FAILED.name)
            false
        }
    }
    
    /**
     * Download server categories to local database
     * Process in deterministic order to ensure consistent category-expense relationships
     */
    private suspend fun downloadServerCategories(): Boolean {
        Log.d(TAG, "Starting category download sync")
        
        return try {
            // Get all categories from server
            val response = categoryApiService.getAllCategories()
            
            if (response.isSuccessful) {
                val serverCategories = response.body()?.data ?: emptyList()
                Log.i(TAG, "Downloaded ${serverCategories.size} categories from server")
                
                // Sort server categories by name to ensure deterministic processing order
                val sortedServerCategories = serverCategories.sortedBy { it.name }
                
                // Process server categories in sorted order
                for (serverCategory in sortedServerCategories) {
                    // Check if we already have this category locally (by ID)
                    val localCategory = categoryDao.getCategoryById(serverCategory.id)
                    
                    if (localCategory == null) {
                        // New category from server - add to local DB with server ID
                        val newEntity = categoryDtoMapper.toEntity(serverCategory)
                        categoryDao.insertCategory(newEntity)
                        
                        Log.d(TAG, "Added new category from server: ${serverCategory.name} (ID: ${serverCategory.id})")
                        
                    } else {
                        // Existing category - check for conflicts
                        handleCategoryPotentialConflict(localCategory, serverCategory)
                    }
                }
                
                Log.i(TAG, "Category download sync completed successfully")
                true
            } else {
                Log.w(TAG, "Category download failed with code: ${response.code()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Category download sync failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Handle potential conflicts between local and server category data
     */
    private suspend fun handleCategoryPotentialConflict(
        localCategory: CategoryEntity,
        serverCategory: com.example.spenttracker.data.remote.dto.CategoryDto
    ) {
        // Simple conflict resolution: server wins if local is already synced
        // More sophisticated strategies can be implemented later
        
        if (localCategory.syncStatus == CategorySyncStatus.SYNCED.name && !localCategory.needsSync) {
            // Update local with server data
            val updatedEntity = categoryDtoMapper.toEntity(serverCategory).copy(
                id = localCategory.id
            )
            
            categoryDao.updateCategory(updatedEntity)
            Log.d(TAG, "Updated category from server: ${serverCategory.name}")
        } else {
            // Local has pending changes - mark as conflict for manual resolution
            categoryDao.updateSyncStatus(localCategory.id, CategorySyncStatus.CONFLICT.name)
            Log.w(TAG, "Conflict detected for category: ${localCategory.name}")
        }
    }
    
    /**
     * Download server changes to local database
     */
    private suspend fun downloadServerChanges(userId: Long): Boolean {
        Log.d(TAG, "Starting download sync for user: $userId")
        
        return try {
            // Get all expenses from server
            val response = expenseApiService.getAllExpenses()
            
            if (response.isSuccessful) {
                val serverExpenses = response.body()?.data ?: emptyList()
                Log.i(TAG, "Downloaded ${serverExpenses.size} expenses from server")
                
                // Process server expenses
                for (serverExpense in serverExpenses) {
                    // Check if we already have this expense locally
                    val localExpense = expenseDao.getExpenseByServerId(serverExpense.id, userId)
                    
                    if (localExpense == null) {
                        // New expense from server - add to local DB
                        val newEntity = expenseDtoMapper.toEntity(serverExpense, userId).copy(
                            serverId = serverExpense.id,
                            syncStatus = SyncStatus.SYNCED.name,
                            needsSync = false,
                            lastSyncAt = Instant.now().toString()
                        )
                        
                        expenseDao.insertExpense(newEntity)
                        Log.d(TAG, "Added new expense from server: ${serverExpense.description}")
                        
                    } else {
                        // Existing expense - check for conflicts
                        handlePotentialConflict(localExpense, serverExpense)
                    }
                }
                
                // Check for expenses deleted on server (present locally but missing from server response)
                val serverExpenseIds = serverExpenses.map { it.id }.toSet()
                val localSyncedExpenses = expenseDao.getExpensesBySyncStatus(SyncStatus.SYNCED.name)
                    .filter { it.userId == userId.toInt() && it.serverId != null }
                
                for (localExpense in localSyncedExpenses) {
                    if (localExpense.serverId !in serverExpenseIds) {
                        // This expense exists locally but not on server - it was deleted on server
                        expenseDao.deleteExpenseById(localExpense.id)
                        Log.d(TAG, "Deleted expense removed from server: ${localExpense.description}")
                    }
                }
                
                Log.i(TAG, "Download sync completed successfully")
                true
            } else {
                Log.w(TAG, "Download failed with code: ${response.code()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download sync failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Handle potential conflicts between local and server data
     */
    private suspend fun handlePotentialConflict(
        localExpense: ExpenseEntity,
        serverExpense: com.example.spenttracker.data.remote.dto.ExpenseDto
    ) {
        // Simple conflict resolution: server wins if local is already synced
        // More sophisticated strategies can be implemented later
        
        if (localExpense.syncStatus == SyncStatus.SYNCED.name && !localExpense.needsSync) {
            // Update local with server data
            val updatedEntity = expenseDtoMapper.toEntity(serverExpense, localExpense.userId.toLong()).copy(
                id = localExpense.id,
                serverId = serverExpense.id,
                syncStatus = SyncStatus.SYNCED.name,
                needsSync = false,
                lastSyncAt = Instant.now().toString()
            )
            
            expenseDao.updateExpense(updatedEntity)
            Log.d(TAG, "Updated expense from server: ${serverExpense.description}")
        } else {
            // Local has pending changes - mark as conflict for manual resolution
            expenseDao.updateSyncStatus(localExpense.id, SyncStatus.CONFLICT.name)
            Log.w(TAG, "Conflict detected for expense: ${localExpense.description}")
        }
    }
    
    /**
     * Get sync statistics for UI display
     */
    suspend fun getSyncStats(): SyncStats {
        val userId = userContextProvider.getCurrentUserId() ?: return SyncStats.empty()
        
        return SyncStats(
            totalExpenses = expenseDao.getExpenseCountByUser(userId),
            syncedExpenses = expenseDao.getExpenseCountByStatus(userId, SyncStatus.SYNCED.name),
            pendingExpenses = expenseDao.getExpenseCountByStatus(userId, SyncStatus.PENDING.name),
            failedExpenses = expenseDao.getExpenseCountByStatus(userId, SyncStatus.FAILED.name),
            conflictExpenses = expenseDao.getExpenseCountByStatus(userId, SyncStatus.CONFLICT.name)
        )
    }
    
    /**
     * Set sync callbacks
     */
    fun setOnSyncSuccess(callback: () -> Unit) {
        onSyncSuccess = callback
    }
    
    fun setOnSyncError(callback: (String) -> Unit) {
        onSyncError = callback
    }
    
    /**
     * Cancel ongoing sync
     */
    fun cancelSync() {
        syncJob?.cancel()
        _syncState.value = SyncState.Idle
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        syncJob?.cancel()
        syncScope.cancel()
    }
}

/**
 * Sync state sealed class
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Sync statistics data class
 */
data class SyncStats(
    val totalExpenses: Int,
    val syncedExpenses: Int,
    val pendingExpenses: Int,
    val failedExpenses: Int,
    val conflictExpenses: Int
) {
    companion object {
        fun empty() = SyncStats(0, 0, 0, 0, 0)
    }
    
    val syncPercentage: Int
        get() = if (totalExpenses > 0) (syncedExpenses * 100 / totalExpenses) else 100
}