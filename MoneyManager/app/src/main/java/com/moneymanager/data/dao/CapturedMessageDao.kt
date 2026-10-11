package com.moneymanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneymanager.data.entity.CapturedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedMessageDao {

    /** Returns the new row id, or -1 when this message was already captured. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(message: CapturedMessageEntity): Long

    /** Restore path: keeps ids and replaces on a hash clash. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: CapturedMessageEntity)

    @Query("SELECT * FROM captured_messages WHERE messageHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): CapturedMessageEntity?

    @Query("SELECT * FROM captured_messages WHERE refHash = :refHash AND status != 'rejected' ORDER BY messageDate")
    suspend fun findByRefHash(refHash: String): List<CapturedMessageEntity>

    /** Candidates for semantic duplicate checks: same amount, close in time, not rejected. */
    @Query("""
        SELECT * FROM captured_messages
        WHERE amount IS NOT NULL AND ABS(amount - :amount) < 0.005
          AND messageDate BETWEEN :from AND :to AND status != 'rejected'
        ORDER BY messageDate
    """)
    suspend fun findInWindow(amount: Double, from: Long, to: Long): List<CapturedMessageEntity>

    @Query("SELECT * FROM captured_messages WHERE status = 'pending' ORDER BY messageDate DESC")
    fun observePending(): Flow<List<CapturedMessageEntity>>

    @Query("SELECT * FROM captured_messages ORDER BY id")
    suspend fun getAll(): List<CapturedMessageEntity>

    @Query("""
        UPDATE captured_messages
        SET status = :status, transactionId = :transactionId, mergedIntoId = :mergedIntoId,
            draftJson = :draftJson, updatedAt = :now
        WHERE id = :id
    """)
    suspend fun updateState(
        id: Long,
        status: String,
        transactionId: Long? = null,
        mergedIntoId: Long? = null,
        draftJson: String? = null,
        now: Long = System.currentTimeMillis(),
    )

    @Query("SELECT MAX(updatedAt) FROM captured_messages")
    suspend fun getLatestTimestamp(): Long?
}
