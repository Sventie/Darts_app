package com.dartsapp.domain.model

import com.dartsapp.data.model.ScoreMultiplier

data class DartInput(
    val field: Int,
    val multiplier: ScoreMultiplier,
    val scoreValue: Int,
    val tapX: Float? = null,   // normalised board coord: 0,0 = centre, 1.0 = canvas half-width
    val tapY: Float? = null
)

data class ActivePlayer(
    val playerId: Long,
    val playerName: String,
    val participantId: Long,
    val remainingScore: Int,
    val currentRoundDarts: List<DartInput> = emptyList(),
    val scoreBeforeRound: Int = 0,
    val placement: Int? = null
)

enum class GameStatus { IN_PROGRESS, FINISHED }

data class ActiveGame(
    val gameId: Long,
    val config: GameConfig,
    val players: List<ActivePlayer>,
    val currentPlayerIndex: Int,
    val roundNumber: Int,
    val status: GameStatus = GameStatus.IN_PROGRESS
) {
    val currentPlayer: ActivePlayer get() = players[currentPlayerIndex]
}
