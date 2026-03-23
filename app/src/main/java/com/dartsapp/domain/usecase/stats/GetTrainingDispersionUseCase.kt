package com.dartsapp.domain.usecase.stats

import com.dartsapp.data.db.dao.TrainingThrowDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.sqrt

class GetTrainingDispersionUseCase @Inject constructor(
    private val trainingThrowDao: TrainingThrowDao
) {
    companion object {
        private const val R_DOUBLE_OUT = 0.894f
    }

    /**
     * Returns the normalised RMS distance between each training throw and its
     * target field centre, divided by [R_DOUBLE_OUT] so that 1.0 means
     * "average miss by a full board radius".
     *
     * Only throws recorded via the dartboard (with known tap positions) are
     * counted; all Zielfeld and Around-the-Clock sessions contribute.
     */
    operator fun invoke(playerId: Long, fromSession: Int = 1, toSession: Int = Int.MAX_VALUE): Flow<Float> {
        val throws = if (fromSession == 1 && toSession == Int.MAX_VALUE)
            trainingThrowDao.getForPlayer(playerId)
        else
            trainingThrowDao.getForPlayerInSessionRange(
                playerId,
                fromOffset = fromSession - 2,
                toOffset   = toSession - 1
            )
        return throws.map { list ->
            if (list.isEmpty()) 0f
            else {
                val meanSqDist = list.map { t ->
                    val dx = t.actualNx - t.targetNx
                    val dy = t.actualNy - t.targetNy
                    dx * dx + dy * dy
                }.average().toFloat()
                (sqrt(meanSqDist) / R_DOUBLE_OUT).coerceIn(0f, 1f)
            }
        }
    }

    fun throwCount(playerId: Long, fromSession: Int = 1, toSession: Int = Int.MAX_VALUE): Flow<Int> =
        if (fromSession == 1 && toSession == Int.MAX_VALUE)
            trainingThrowDao.getCountForPlayer(playerId)
        else
            trainingThrowDao.getCountForPlayerInSessionRange(
                playerId,
                fromOffset = fromSession - 2,
                toOffset   = toSession - 1
            )
}
