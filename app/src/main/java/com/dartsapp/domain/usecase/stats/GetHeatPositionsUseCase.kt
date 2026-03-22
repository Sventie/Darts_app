package com.dartsapp.domain.usecase.stats

import com.dartsapp.data.db.dao.DartThrowDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHeatPositionsUseCase @Inject constructor(
    private val dartThrowDao: DartThrowDao
) {
    data class HitPosition(val nx: Float, val ny: Float)

    /**
     * Returns tap positions for [playerId], optionally restricted to games
     * [fromGame]..[toGame] ordered chronologically (1-based, inclusive).
     * Pass the defaults to get all games.
     */
    operator fun invoke(
        playerId: Long,
        fromGame: Int = 1,
        toGame: Int   = Int.MAX_VALUE
    ): Flow<List<HitPosition>> {
        val offset = (fromGame - 1).coerceAtLeast(0)
        val count  = if (toGame == Int.MAX_VALUE) Int.MAX_VALUE
                     else (toGame - fromGame + 1).coerceAtLeast(0)

        return if (fromGame == 1 && toGame == Int.MAX_VALUE) {
            dartThrowDao.getTapPositionsForPlayer(playerId)
        } else {
            dartThrowDao.getTapPositionsForPlayerInRange(playerId, count, offset)
        }.map { rows -> rows.map { HitPosition(it.tapX, it.tapY) } }
    }

    fun gameCount(playerId: Long): Flow<Int> =
        dartThrowDao.getGameCountForPlayer(playerId)
}
