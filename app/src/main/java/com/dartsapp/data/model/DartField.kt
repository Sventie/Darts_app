package com.dartsapp.data.model

sealed class DartField(val baseValue: Int) {
    object Miss : DartField(0)
    data class Sector(val number: Int) : DartField(number)
    object Bull : DartField(25)
    object Bullseye : DartField(50)

    companion object {
        fun fromInt(value: Int): DartField = when (value) {
            0 -> Miss
            in 1..20 -> Sector(value)
            25 -> Bull
            50 -> Bullseye
            else -> throw IllegalArgumentException("Invalid dart field: $value")
        }
    }
}
