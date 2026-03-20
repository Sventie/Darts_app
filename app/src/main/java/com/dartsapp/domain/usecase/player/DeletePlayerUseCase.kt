package com.dartsapp.domain.usecase.player

import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.entity.PlayerEntity
import javax.inject.Inject

class DeletePlayerUseCase @Inject constructor(
    private val playerDao: PlayerDao
) {
    suspend operator fun invoke(player: PlayerEntity) {
        playerDao.delete(player)
    }
}
