package com.dartsapp.data.repository

import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.FieldFrequencyRow
import com.dartsapp.data.db.dao.PlayerStatsRaw
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val dartThrowDao: DartThrowDao
) {
    fun getPlayerStatsRaw(playerId: Long): Flow<PlayerStatsRaw?> =
        dartThrowDao.getPlayerStatsRaw(playerId)

    fun getFieldFrequency(playerId: Long): Flow<List<FieldFrequencyRow>> =
        dartThrowDao.getFieldFrequencyForPlayer(playerId)
}
