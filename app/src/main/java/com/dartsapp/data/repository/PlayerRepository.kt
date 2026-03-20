package com.dartsapp.data.repository

import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao
) {
    fun getAllPlayers(): Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    suspend fun getPlayerById(id: Long): PlayerEntity? = playerDao.getPlayerById(id)

    suspend fun insertPlayer(player: PlayerEntity): Long = playerDao.insert(player)

    suspend fun deletePlayer(player: PlayerEntity) = playerDao.delete(player)

    suspend fun getPlayerByName(name: String): PlayerEntity? = playerDao.getPlayerByName(name)
}
