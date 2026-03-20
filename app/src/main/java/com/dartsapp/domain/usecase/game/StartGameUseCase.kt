package com.dartsapp.domain.usecase.game

import com.dartsapp.data.db.dao.GameDao
import com.dartsapp.data.db.dao.GameParticipantDao
import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.entity.GameEntity
import com.dartsapp.data.db.entity.GameParticipantEntity
import com.dartsapp.domain.model.ActiveGame
import com.dartsapp.domain.model.ActivePlayer
import com.dartsapp.domain.model.GameConfig
import javax.inject.Inject

class StartGameUseCase @Inject constructor(
    private val gameDao: GameDao,
    private val gameParticipantDao: GameParticipantDao,
    private val playerDao: PlayerDao
) {
    suspend operator fun invoke(config: GameConfig): ActiveGame {
        val now = System.currentTimeMillis()
        val gameId = gameDao.insert(
            GameEntity(
                startingScore = config.startingScore,
                closeCondition = config.closeCondition.name,
                startedAt = now
            )
        )

        val participants = config.playerIds.mapIndexed { index, playerId ->
            GameParticipantEntity(
                gameId = gameId,
                playerId = playerId,
                turnOrder = index
            )
        }
        val participantIds = gameParticipantDao.insertAll(participants)

        val activePlayers = config.playerIds.mapIndexed { index, playerId ->
            val player = requireNotNull(playerDao.getPlayerById(playerId)) {
                "Player $playerId not found"
            }
            ActivePlayer(
                playerId = playerId,
                playerName = player.name,
                participantId = participantIds[index],
                remainingScore = config.startingScore,
                scoreBeforeRound = config.startingScore
            )
        }

        return ActiveGame(
            gameId = gameId,
            config = config,
            players = activePlayers,
            currentPlayerIndex = 0,
            roundNumber = 1
        )
    }
}
