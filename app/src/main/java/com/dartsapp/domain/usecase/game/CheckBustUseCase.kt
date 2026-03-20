package com.dartsapp.domain.usecase.game

import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.CloseCondition
import com.dartsapp.domain.model.DartInput
import javax.inject.Inject

sealed class BustResult {
    data class Continue(val remaining: Int) : BustResult()
    data class Win(val dartsUsed: Int) : BustResult()
    data class Bust(val reason: BustReason) : BustResult()
}

enum class BustReason {
    OVERSHOOT,
    UNREACHABLE_ONE,
    NOT_DOUBLE_OUT
}

class CheckBustUseCase @Inject constructor() {
    operator fun invoke(
        currentScore: Int,
        roundDarts: List<DartInput>,
        closeCondition: CloseCondition
    ): BustResult {
        var running = currentScore
        for ((index, dart) in roundDarts.withIndex()) {
            running -= dart.scoreValue
            if (running < 0) return BustResult.Bust(BustReason.OVERSHOOT)
            if (running == 1 && closeCondition == CloseCondition.DOUBLE_OUT) {
                return BustResult.Bust(BustReason.UNREACHABLE_ONE)
            }
            if (running == 0) {
                return when (closeCondition) {
                    CloseCondition.DOUBLE_OUT -> {
                        if (dart.multiplier == ScoreMultiplier.DOUBLE)
                            BustResult.Win(dartsUsed = index + 1)
                        else
                            BustResult.Bust(BustReason.NOT_DOUBLE_OUT)
                    }
                    CloseCondition.SINGLE_OUT -> BustResult.Win(dartsUsed = index + 1)
                }
            }
        }
        return BustResult.Continue(remaining = running)
    }
}
