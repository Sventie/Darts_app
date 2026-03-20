package com.dartsapp.domain.model

data class GameConfig(
    val playerIds: List<Long>,
    val startingScore: Int,
    val closeCondition: CloseCondition
) {
    companion object {
        val validStartingScores = listOf(101, 201, 301, 401, 501)
    }
}

enum class CloseCondition {
    DOUBLE_OUT,
    SINGLE_OUT
}
