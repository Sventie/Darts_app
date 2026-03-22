package com.dartsapp.data.repository

import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.FieldFrequencyRow
import com.dartsapp.data.db.dao.GameStatsRaw
import com.dartsapp.data.db.dao.DartCountRaw
import com.dartsapp.data.db.dao.RoundStatsRaw
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val dartThrowDao: DartThrowDao
) {
    fun getGameStats(playerId: Long): Flow<GameStatsRaw> =
        dartThrowDao.getGameStatsForPlayer(playerId)

    fun getDartStats(playerId: Long): Flow<DartCountRaw> =
        dartThrowDao.getDartStatsForPlayer(playerId)

    fun getAvgScorePerRound(playerId: Long): Flow<Double> =
        dartThrowDao.getAvgScorePerRound(playerId)

    fun getRoundStats(playerId: Long): Flow<RoundStatsRaw> =
        dartThrowDao.getRoundStatsForPlayer(playerId)

    fun getFieldFrequency(playerId: Long): Flow<List<FieldFrequencyRow>> =
        dartThrowDao.getFieldFrequencyForPlayer(playerId)
}
