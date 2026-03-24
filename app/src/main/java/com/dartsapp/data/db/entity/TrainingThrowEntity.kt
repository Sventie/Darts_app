package com.dartsapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single dart throw recorded during Zielfeld or Around the Clock training.
 * Only throws made via the dartboard (with known tap coordinates) are stored.
 *
 * [targetNx] / [targetNy]: normalised board coordinates of the target field centre.
 * [actualNx] / [actualNy]: where the player actually hit (same coordinate space).
 */
@Entity(
    tableName = "training_throws",
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
data class TrainingThrowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "player_id") val playerId: Long,
    @ColumnInfo(name = "target_nx") val targetNx: Float,
    @ColumnInfo(name = "target_ny") val targetNy: Float,
    @ColumnInfo(name = "actual_nx") val actualNx: Float,
    @ColumnInfo(name = "actual_ny") val actualNy: Float,
    @ColumnInfo(name = "thrown_at") val thrownAt: Long
)
