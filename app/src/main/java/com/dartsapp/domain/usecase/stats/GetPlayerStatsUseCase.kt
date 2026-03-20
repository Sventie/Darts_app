package com.dartsapp.domain.usecase.stats

import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.domain.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPlayerStatsUseCase @Inject constructor(
    private val dartThrowDao: DartThrowDao,
    private val playerDao: PlayerDao
) {
    operator fun invoke(playerId: Long): Flow<PlayerStats?> {
        return dartThrowDao.getPlayerStatsRaw(playerId).map { raw ->
            if (raw == null) return@map null
            val player = playerDao.getPlayerById(playerId) ?: return@map null
            PlayerStats(
                playerId = playerId,
                playerName = player.name,
                gamesPlayed = raw.gamesPlayed,
                wins = raw.wins,
                avgScorePerDart = raw.avgScorePerDart,
                avgScorePerRound = raw.avgScorePerDart * 3,
                highestRound = raw.highestRound,
                totalDartsThrown = raw.totalDartsThrown
            )
        }
    }
}
