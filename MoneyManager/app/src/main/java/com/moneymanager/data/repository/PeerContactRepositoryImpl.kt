package com.moneymanager.data.repository

import com.moneymanager.data.dao.PeerContactDao
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.domain.repository.PeerContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerContactRepositoryImpl @Inject constructor(
    private val peerContactDao: PeerContactDao
) : PeerContactRepository {

    override fun getAllPeers(): Flow<List<PeerContact>> =
        peerContactDao.getAllPeers()

    override fun getPeerById(id: Long): Flow<PeerContact?> =
        peerContactDao.getPeerByIdFlow(id)

    override suspend fun getPeerByIdSync(id: Long): PeerContact? =
        peerContactDao.getPeerById(id)

    override fun getTotalLent(): Flow<Double?> =
        peerContactDao.getTotalLent()

    override fun getTotalReceived(): Flow<Double?> =
        peerContactDao.getTotalReceived()

    override suspend fun insertPeer(peer: PeerContact): Long =
        peerContactDao.insertPeer(peer)

    override suspend fun updatePeer(peer: PeerContact) =
        peerContactDao.updatePeer(peer)

    override suspend fun deletePeer(peer: PeerContact) =
        peerContactDao.deletePeer(peer)
}