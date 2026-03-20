package com.dartsapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["winner_player_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("winner_player_id")]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "starting_score") val startingScore: Int,
    @ColumnInfo(name = "close_condition") val closeCondition: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    @ColumnInfo(name = "winner_player_id") val winnerPlayerId: Long? = null
)
