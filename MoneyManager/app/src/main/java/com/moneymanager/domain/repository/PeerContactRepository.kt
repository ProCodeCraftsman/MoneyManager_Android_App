package com.moneymanager.domain.repository

import com.moneymanager.data.entity.PeerContact
import kotlinx.coroutines.flow.Flow

interface PeerContactRepository {
    fun getAllPeers(): Flow<List<PeerContact>>
    fun getPeerById(id: Long): Flow<PeerContact?>
    suspend fun getPeerByIdSync(id: Long): PeerContact?
    suspend fun getPeerByName(name: String): PeerContact?
    fun getTotalOutstanding(): Flow<Double?>
    fun getTotalLent(): Flow<Double?>
    fun getTotalReceived(): Flow<Double?>
    suspend fun insertPeer(peer: PeerContact): Long
    suspend fun updatePeer(peer: PeerContact)
    suspend fun deletePeer(peer: PeerContact)
    suspend fun deletePeerById(id: Long)
}