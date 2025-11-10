package com.example.spenttracker.data.local

import androidx.room.*
import com.example.spenttracker.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Income entities
 * Provides database operations for income management
 */
@Dao
interface IncomeDao {

    /**
     * Get all incomes for a specific user, ordered by date descending
     */
    @Query("SELECT * FROM incomes WHERE user_id = :userId ORDER BY date DESC")
    fun getAllIncomes(userId: Long): Flow<List<IncomeEntity>>

    /**
     * Get incomes for a specific period
     */
    @Query("""
        SELECT * FROM incomes
        WHERE user_id = :userId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date DESC
    """)
    fun getIncomesForPeriod(
        userId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<IncomeEntity>>

    /**
     * Get total income for a specific period
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM incomes
        WHERE user_id = :userId
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalIncomeForPeriod(
        userId: Long,
        startDate: String,
        endDate: String
    ): Double

    /**
     * Get income by ID
     */
    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    suspend fun getIncomeById(incomeId: Int): IncomeEntity?

    /**
     * Get income by server ID
     */
    @Query("SELECT * FROM incomes WHERE server_id = :serverId")
    suspend fun getIncomeByServerId(serverId: Long): IncomeEntity?

    /**
     * Get all recurring incomes
     */
    @Query("SELECT * FROM incomes WHERE user_id = :userId AND is_recurring = 1")
    fun getRecurringIncomes(userId: Long): Flow<List<IncomeEntity>>

    /**
     * Get incomes by source
     */
    @Query("SELECT * FROM incomes WHERE user_id = :userId AND source = :source ORDER BY date DESC")
    fun getIncomesBySource(userId: Long, source: String): Flow<List<IncomeEntity>>

    /**
     * Get all unique income sources for a user
     */
    @Query("SELECT DISTINCT source FROM incomes WHERE user_id = :userId ORDER BY source ASC")
    fun getIncomeSources(userId: Long): Flow<List<String>>

    /**
     * Insert a new income
     * Returns the row ID of the newly inserted income
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    /**
     * Insert multiple incomes
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>)

    /**
     * Update an existing income
     */
    @Update
    suspend fun updateIncome(income: IncomeEntity)

    /**
     * Delete an income
     */
    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    /**
     * Delete income by ID
     */
    @Query("DELETE FROM incomes WHERE id = :incomeId")
    suspend fun deleteIncomeById(incomeId: Int)

    /**
     * Get unsynced incomes (for background sync)
     */
    @Query("SELECT * FROM incomes WHERE needs_sync = 1 ORDER BY updated_at DESC")
    suspend fun getUnsyncedIncomes(): List<IncomeEntity>

    /**
     * Mark income as synced
     */
    @Query("""
        UPDATE incomes
        SET needs_sync = 0,
            sync_status = 'SYNCED',
            last_sync_at = :syncTime
        WHERE id = :incomeId
    """)
    suspend fun markAsSynced(incomeId: Int, syncTime: String)

    /**
     * Update server ID for a local income after successful sync
     */
    @Query("UPDATE incomes SET server_id = :serverId WHERE id = :localId")
    suspend fun updateServerId(localId: Int, serverId: Long)

    /**
     * Get income count for statistics
     */
    @Query("SELECT COUNT(*) FROM incomes WHERE user_id = :userId")
    suspend fun getIncomeCount(userId: Long): Int

    /**
     * Delete all incomes for a user (for logout/reset)
     */
    @Query("DELETE FROM incomes WHERE user_id = :userId")
    suspend fun deleteAllIncomes(userId: Long)
}
