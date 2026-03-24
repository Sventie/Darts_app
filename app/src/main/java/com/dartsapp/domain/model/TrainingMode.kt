package com.dartsapp.domain.model

enum class TrainingMode {
    ZIELFELD,
    AROUND_THE_CLOCK,
    SCORING_ROUNDS;

    fun displayName(): String = when (this) {
        ZIELFELD -> "Zielfeld"
        AROUND_THE_CLOCK -> "Around the Clock"
        SCORING_ROUNDS -> "Scoring Rounds"
    }

    fun description(): String = when (this) {
        ZIELFELD -> "Triff 10 vorgegebene Felder"
        AROUND_THE_CLOCK -> "Alle Zahlen 1–20 der Reihe nach"
        SCORING_ROUNDS -> "10 Runden, maximaler Score"
    }
}

enum class TrainingDifficulty {
    BEGINNER,
    INTERMEDIATE,
    PRO;

    fun displayName(): String = when (this) {
        BEGINNER -> "Anfänger"
        INTERMEDIATE -> "Fortgeschritten"
        PRO -> "Profi"
    }

    fun targetAverage(): Int = when (this) {
        BEGINNER -> 45
        INTERMEDIATE -> 60
        PRO -> 80
    }
}

fun generateTargetFields(difficulty: TrainingDifficulty, count: Int = 10): List<String> {
    val pool = buildList {
        // Singles always included
        for (i in 1..20) add("S$i")
        add("Bull")
        when (difficulty) {
            TrainingDifficulty.BEGINNER -> { /* singles + bull only */ }
            TrainingDifficulty.INTERMEDIATE -> {
                for (i in 1..20) add("D$i")
            }
            TrainingDifficulty.PRO -> {
                for (i in 1..20) add("D$i")
                // Triples weighted 2x
                for (i in 1..20) { add("T$i"); add("T$i") }
                add("Bullseye")
            }
        }
    }
    return pool.shuffled().take(count)
}

fun requiresDouble(number: Int, difficulty: TrainingDifficulty): Boolean = when (difficulty) {
    TrainingDifficulty.BEGINNER -> false
    TrainingDifficulty.INTERMEDIATE -> number % 5 == 0
    TrainingDifficulty.PRO -> true
}
