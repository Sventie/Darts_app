package com.dartsapp.data.repository

import com.dartsapp.data.db.dao.GameDao
import com.dartsapp.data.db.dao.GameParticipantDao
import com.dartsapp.data.db.dao.RoundDao
import com.dartsapp.data.db.entity.GameEntity
import com.dartsapp.data.db.entity.GameParticipantEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val gameParticipantDao: GameParticipantDao,
    private val roundDao: RoundDao
) {
    suspend fun getGameById(gameId: Long): GameEntity? = gameDao.getGameById(gameId)

    suspend fun finishGame(gameId: Long, winnerPlayerId: Long) {
        gameDao.finishGame(gameId, System.currentTimeMillis(), winnerPlayerId)
    }

    suspend fun getParticipantsForGame(gameId: Long): List<GameParticipantEntity> =
        gameParticipantDao.getParticipantsForGame(gameId)

    suspend fun updatePlacement(participantId: Long, placement: Int) {
        gameParticipantDao.updatePlacement(participantId, placement)
    }

    suspend fun getLastGamePlayerIds(): List<Long> =
        gameParticipantDao.getLastGamePlayerIds()

    suspend fun undoRound(roundId: Long, participantId: Long, scoreBefore: Int) {
        // CASCADE on dart_throws means deleting the round also deletes its throws
        roundDao.deleteById(roundId)
        gameParticipantDao.updateFinalScore(participantId, scoreBefore)
    }

    suspend fun undoWinRound(
        roundId: Long,
        participantId: Long,
        scoreBefore: Int,
        gameId: Long,
        wasFirstPlace: Boolean
    ) {
        roundDao.deleteById(roundId)
        gameParticipantDao.updateFinalScore(participantId, scoreBefore)
        gameParticipantDao.clearPlacement(participantId)
        if (wasFirstPlace) {
            gameDao.unfinishGame(gameId)
        }
    }
}
