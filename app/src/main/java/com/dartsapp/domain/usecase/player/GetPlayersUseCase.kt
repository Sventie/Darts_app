package com.dartsapp.domain.usecase.player

import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayersUseCase @Inject constructor(
    private val playerDao: PlayerDao
) {
    operator fun invoke(): Flow<List<PlayerEntity>> = playerDao.getAllPlayers()
}
