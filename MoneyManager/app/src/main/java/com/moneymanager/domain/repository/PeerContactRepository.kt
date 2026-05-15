package com.moneymanager.domain.repository

import com.moneymanager.data.entity.PeerContact
import kotlinx.coroutines.flow.Flow

interface PeerContactRepository {
    fun getAllPeers(): Flow<List<PeerContact>>
    fun getPeerById(id: Long): Flow<PeerContact?>
    suspend fun getPeerByIdSync(id: Long): PeerContact?
    suspend fun getPeerByLookupKey(lookupKey: String): PeerContact?
    fun getPeerByLookupKeyFlow(lookupKey: String): Flow<PeerContact?>
    fun getTotalLent(): Flow<Double?>
    fun getTotalReceived(): Flow<Double?>
    suspend fun insertPeer(peer: PeerContact): Long
    suspend fun updatePeer(peer: PeerContact)
    suspend fun deletePeer(peer: PeerContact)
}