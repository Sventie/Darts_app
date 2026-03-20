package com.dartsapp.data.model

data class DartScore(
    val dartField: DartField,
    val multiplier: ScoreMultiplier
) {
    val value: Int get() = dartField.baseValue * multiplier.value
}
