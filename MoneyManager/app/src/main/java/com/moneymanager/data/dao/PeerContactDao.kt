package com.moneymanager.data.dao

import androidx.room.*
import com.moneymanager.data.entity.PeerContact
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerContactDao {
    @Query("SELECT * FROM peer_contacts ORDER BY displayName ASC")
    fun getAllPeers(): Flow<List<PeerContact>>

    @Query("SELECT * FROM peer_contacts WHERE id = :id")
    suspend fun getPeerById(id: Long): PeerContact?

    @Query("SELECT * FROM peer_contacts WHERE id = :id")
    fun getPeerByIdFlow(id: Long): Flow<PeerContact?>

    @Query("SELECT SUM(totalGiven) FROM peer_contacts")
    fun getTotalLent(): Flow<Double?>

    @Query("SELECT SUM(totalReceived) FROM peer_contacts")
    fun getTotalReceived(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerContact): Long

    @Update
    suspend fun updatePeer(peer: PeerContact)

    @Delete
    suspend fun deletePeer(peer: PeerContact)

}