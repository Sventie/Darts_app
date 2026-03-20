package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dartsapp.data.db.entity.DartThrowEntity
import kotlinx.coroutines.flow.Flow

data class FieldFrequencyRow(
    val field: Int,
    val multiplier: String,
    val count: Int
)

data class PlayerStatsRaw(
    val playerId: Long,
    val gamesPlayed: Int,
    val wins: Int,
    val avgScorePerDart: Double,
    val totalDartsThrown: Int,
    val highestRound: Int
)

@Dao
interface DartThrowDao {
    @Insert
    suspend fun insert(dartThrow: DartThrowEntity): Long

    @Insert
    suspend fun insertAll(dartThrows: List<DartThrowEntity>)

    @Query("""
        SELECT dt.field, dt.multiplier, COUNT(*) as count
        FROM dart_throws dt
        JOIN rounds r ON r.id = dt.round_id
        JOIN game_participants gp ON gp.id = r.game_participant_id
        WHERE gp.player_id = :playerId AND r.was_bust = 0
        GROUP BY dt.field, dt.multiplier
        ORDER BY dt.field ASC
    """)
    fun getFieldFrequencyForPlayer(playerId: Long): Flow<List<FieldFrequencyRow>>

    @Query("""
        SELECT
            p.id as playerId,
            COUNT(DISTINCT gp.game_id) as gamesPlayed,
            SUM(CASE WHEN gp.placement = 1 THEN 1 ELSE 0 END) as wins,
            COALESCE(AVG(dt.score_value), 0.0) as avgScorePerDart,
            COUNT(dt.id) as totalDartsThrown,
            COALESCE(MAX(r_max.round_total), 0) as highestRound
        FROM players p
        LEFT JOIN game_participants gp ON gp.player_id = p.id
        LEFT JOIN rounds r ON r.game_participant_id = gp.id AND r.was_bust = 0
        LEFT JOIN dart_throws dt ON dt.round_id = r.id
        LEFT JOIN (
            SELECT r2.id, SUM(dt2.score_value) as round_total
            FROM rounds r2
            JOIN dart_throws dt2 ON dt2.round_id = r2.id
            WHERE r2.was_bust = 0
            GROUP BY r2.id
        ) r_max ON r_max.id = r.id
        WHERE p.id = :playerId
        GROUP BY p.id
    """)
    fun getPlayerStatsRaw(playerId: Long): Flow<PlayerStatsRaw>
}
