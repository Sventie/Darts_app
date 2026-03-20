package com.dartsapp.data.model

data class DartScore(
    val field: DartField,
    val multiplier: ScoreMultiplier
) {
    val value: Int get() = field.baseValue * multiplier.value
}
