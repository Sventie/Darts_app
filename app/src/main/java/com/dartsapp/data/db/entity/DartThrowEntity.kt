package com.dartsapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dart_throws",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["round_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("round_id"), Index("field")]
)
data class DartThrowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "round_id") val roundId: Long,
    @ColumnInfo(name = "dart_position") val dartPosition: Int,
    val field: Int,
    val multiplier: String,
    @ColumnInfo(name = "score_value") val scoreValue: Int,
    @ColumnInfo(name = "is_padding") val isPadding: Boolean = false,
    @ColumnInfo(name = "tap_x") val tapX: Float? = null,
    @ColumnInfo(name = "tap_y") val tapY: Float? = null
)
