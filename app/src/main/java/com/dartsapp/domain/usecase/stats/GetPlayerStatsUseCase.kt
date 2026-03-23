package com.dartsapp.domain.usecase.stats

import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.GameParticipantDao
import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.domain.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetPlayerStatsUseCase @Inject constructor(
    private val dartThrowDao: DartThrowDao,
    private val gameParticipantDao: GameParticipantDao,
    private val playerDao: PlayerDao
) {
    operator fun invoke(playerId: Long): Flow<PlayerStats?> {
        return combine(
            dartThrowDao.getGameStatsForPlayer(playerId),
            dartThrowDao.getDartStatsForPlayer(playerId),
            dartThrowDao.getAvgScorePerRound(playerId),
            dartThrowDao.getRoundStatsForPlayer(playerId),
            gameParticipantDao.getSocialStatsForPlayer(playerId)
        ) { game, darts, avgPerRound, rounds, social ->
            val player = playerDao.getPlayerById(playerId) ?: return@combine null
            PlayerStats(
                playerId           = playerId,
                playerName         = player.name,
                gamesPlayed        = game.gamesPlayed,
                wins               = game.wins,
                secondPlace        = game.secondPlace,
                thirdPlace         = game.thirdPlace,
                avgScorePerDart    = darts.avgScorePerDart,
                avgScorePerRound   = avgPerRound,
                first9Average      = darts.first9Average,
                highestCheckout    = game.highestCheckout,
                bustCount          = game.bustCount,
                checkoutAttempts   = game.checkoutAttempts,
                highestRound       = rounds.highestRound,
                roundsUnder10      = rounds.roundsUnder10,
                totalRounds        = rounds.totalRounds,
                totalDartsThrown   = darts.totalDartsThrown,
                doubleHits         = darts.doubleHits,
                tripleHits         = darts.tripleHits,
                outOfBounceCount   = darts.outOfBounceCount,
                bestBuddyName      = social.bestBuddyName,
                rivalName          = social.rivalName,
                easyWinName        = social.easyWinName
            )
        }
    }
}
