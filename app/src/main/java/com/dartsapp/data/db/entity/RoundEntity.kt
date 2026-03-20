package com.dartsapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GameParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_participant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("game_id"), Index("game_participant_id")]
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "game_participant_id") val gameParticipantId: Long,
    @ColumnInfo(name = "round_number") val roundNumber: Int,
    @ColumnInfo(name = "score_before") val scoreBefore: Int,
    @ColumnInfo(name = "score_after") val scoreAfter: Int,
    @ColumnInfo(name = "was_bust") val wasBust: Boolean = false,
    @ColumnInfo(name = "is_winning_round") val isWinningRound: Boolean = false
)
