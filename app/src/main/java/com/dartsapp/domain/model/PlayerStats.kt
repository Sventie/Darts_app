package com.dartsapp.domain.model

data class PlayerStats(
    val playerId: Long,
    val playerName: String,
    val gamesPlayed: Int,
    val wins: Int,
    val avgScorePerDart: Double,
    val avgScorePerRound: Double,
    val highestRound: Int,
    val totalDartsThrown: Int
)

data class FieldHitFrequency(
    val field: Int,
    val singleCount: Int,
    val doubleCount: Int,
    val tripleCount: Int
) {
    val totalHits: Int get() = singleCount + doubleCount + tripleCount
}
