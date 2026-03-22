package com.dartsapp.domain.model

data class PlayerStats(
    val playerId: Long,
    val playerName: String,
    // Game-level
    val gamesPlayed: Int,
    val wins: Int,
    val secondPlace: Int,
    val thirdPlace: Int,
    // Averages (checkout rounds excluded)
    val avgScorePerDart: Double,
    val avgScorePerRound: Double,
    val first9Average: Double,
    // Checkout
    val highestCheckout: Int,
    val bustCount: Int,
    val checkoutAttempts: Int,
    // Rounds
    val highestRound: Int,
    val roundsUnder10: Int,
    val totalRounds: Int,
    // Darts
    val totalDartsThrown: Int,
    val doubleHits: Int,
    val tripleHits: Int,
    val outOfBounceCount: Int,
    // Social
    val bestBuddyName: String?,
    val rivalName: String?,
    val easyWinName: String?
)

data class FieldHitFrequency(
    val field: Int,
    val singleCount: Int,
    val doubleCount: Int,
    val tripleCount: Int
) {
    val totalHits: Int get() = singleCount + doubleCount + tripleCount
}
