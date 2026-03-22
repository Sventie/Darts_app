package com.dartsapp.domain.usecase.game

import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.GameParticipantDao
import com.dartsapp.data.db.dao.RoundDao
import com.dartsapp.data.db.entity.DartThrowEntity
import com.dartsapp.data.db.entity.RoundEntity
import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.DartInput
import javax.inject.Inject

data class ProcessRoundResult(
    val roundId: Long,
    val scoreAfter: Int,
    val isWin: Boolean,
    val wasBust: Boolean
)

class ProcessRoundUseCase @Inject constructor(
    private val roundDao: RoundDao,
    private val dartThrowDao: DartThrowDao,
    private val gameParticipantDao: GameParticipantDao
) {
    suspend operator fun invoke(
        participantId: Long,
        gameId: Long,
        roundNumber: Int,
        scoreBefore: Int,
        darts: List<DartInput>,
        bustResult: BustResult
    ): ProcessRoundResult {
        val wasBust = bustResult is BustResult.Bust
        val isWin = bustResult is BustResult.Win
        val scoreAfter = if (wasBust) scoreBefore else scoreBefore - darts.sumOf { it.scoreValue }

        val roundId = roundDao.insert(
            RoundEntity(
                gameId = gameId,
                gameParticipantId = participantId,
                roundNumber = roundNumber,
                scoreBefore = scoreBefore,
                scoreAfter = scoreAfter,
                wasBust = wasBust,
                isWinningRound = isWin
            )
        )

        // Pad to 3 darts with misses if fewer (e.g. won on dart 1 or 2)
        val actualDartCount = darts.size
        val paddedDarts = darts.toMutableList()
        while (paddedDarts.size < 3) {
            paddedDarts.add(DartInput(field = 0, multiplier = ScoreMultiplier.SINGLE, scoreValue = 0))
        }

        dartThrowDao.insertAll(
            paddedDarts.mapIndexed { i, dart ->
                DartThrowEntity(
                    roundId = roundId,
                    dartPosition = i + 1,
                    field = dart.field,
                    multiplier = dart.multiplier.name,
                    scoreValue = dart.scoreValue,
                    isPadding = i >= actualDartCount,
                    tapX = dart.tapX,
                    tapY = dart.tapY
                )
            }
        )

        gameParticipantDao.updateFinalScore(participantId, scoreAfter)

        return ProcessRoundResult(roundId = roundId, scoreAfter = scoreAfter, isWin = isWin, wasBust = wasBust)
    }
}
