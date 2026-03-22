package com.dartsapp.domain.usecase.stats

import com.dartsapp.data.db.dao.DartThrowDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetHeatPositionsUseCase @Inject constructor(
    private val dartThrowDao: DartThrowDao
) {
    data class HitPosition(val nx: Float, val ny: Float)

    operator fun invoke(playerId: Long): Flow<List<HitPosition>> =
        dartThrowDao.getTapPositionsForPlayer(playerId).map { rows ->
            rows.map { HitPosition(it.tapX, it.tapY) }
        }
}
