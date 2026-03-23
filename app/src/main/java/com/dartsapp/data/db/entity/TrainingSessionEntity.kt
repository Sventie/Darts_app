package com.dartsapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("player_id")]
)
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "player_id") val playerId: Long,
    val mode: String,
    val difficulty: String,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
    /** Total darts for Zielfeld/Around-the-Clock; average×10 for Scoring Rounds */
    val result: Int,
    /** Fields completed: 10 for Zielfeld, 20 for Around-the-Clock, rounds for Scoring Rounds */
    @ColumnInfo(name = "fields_completed") val fieldsCompleted: Int
)
