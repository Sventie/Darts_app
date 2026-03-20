package com.dartsapp.domain.usecase.player

import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.entity.PlayerEntity
import javax.inject.Inject

sealed class CreatePlayerResult {
    data class Success(val playerId: Long) : CreatePlayerResult()
    object NameEmpty : CreatePlayerResult()
    object NameTaken : CreatePlayerResult()
}

class CreatePlayerUseCase @Inject constructor(
    private val playerDao: PlayerDao
) {
    suspend operator fun invoke(name: String): CreatePlayerResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return CreatePlayerResult.NameEmpty
        if (playerDao.getPlayerByName(trimmed) != null) return CreatePlayerResult.NameTaken
        val id = playerDao.insert(PlayerEntity(name = trimmed, createdAt = System.currentTimeMillis()))
        return CreatePlayerResult.Success(id)
    }
}
