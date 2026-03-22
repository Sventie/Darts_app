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

/** Game-level stats: placements, checkout info */
data class GameStatsRaw(
    val gamesPlayed: Int,
    val wins: Int,
    val secondPlace: Int,
    val thirdPlace: Int,
    val highestCheckout: Int,
    val bustCount: Int,
    val checkoutAttempts: Int
)

/** Dart-level stats: counts, averages per dart, doubles/triples */
data class DartCountRaw(
    val avgScorePerDart: Double,
    val first9Average: Double,
    val totalDartsThrown: Int,
    val doubleHits: Int,
    val tripleHits: Int,
    val outOfBounceCount: Int
)

/** Round-level stats: highest round, rounds under 10 */
data class RoundStatsRaw(
    val highestRound: Int,
    val roundsUnder10: Int,
    val totalRounds: Int
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
        WHERE gp.player_id = :playerId AND r.was_bust = 0 AND dt.is_padding = 0
        GROUP BY dt.field, dt.multiplier
        ORDER BY dt.field ASC
    """)
    fun getFieldFrequencyForPlayer(playerId: Long): Flow<List<FieldFrequencyRow>>

    @Query("""
        SELECT
            COUNT(DISTINCT CASE WHEN g.finished_at IS NOT NULL THEN gp.game_id END) as gamesPlayed,
            COALESCE(SUM(CASE WHEN gp.placement = 1 THEN 1 ELSE 0 END), 0) as wins,
            COALESCE(SUM(CASE WHEN gp.placement = 2 AND (
                SELECT COUNT(*) FROM game_participants gp2 WHERE gp2.game_id = gp.game_id
            ) > 2 THEN 1 ELSE 0 END), 0) as secondPlace,
            COALESCE(SUM(CASE WHEN gp.placement = 3 AND (
                SELECT COUNT(*) FROM game_participants gp3 WHERE gp3.game_id = gp.game_id
            ) > 3 THEN 1 ELSE 0 END), 0) as thirdPlace,
            COALESCE(MAX(CASE WHEN r.is_winning_round = 1 THEN r.score_before END), 0) as highestCheckout,
            COALESCE(SUM(CASE WHEN r.was_bust = 1 THEN 1 ELSE 0 END), 0) as bustCount,
            COALESCE(SUM(CASE WHEN r.was_bust = 1 OR r.is_winning_round = 1 THEN 1 ELSE 0 END), 0) as checkoutAttempts
        FROM game_participants gp
        LEFT JOIN games g ON g.id = gp.game_id
        LEFT JOIN rounds r ON r.game_participant_id = gp.id
        WHERE gp.player_id = :playerId
    """)
    fun getGameStatsForPlayer(playerId: Long): Flow<GameStatsRaw>

    @Query("""
        SELECT
            COALESCE(AVG(CASE WHEN r.is_winning_round = 0 AND r.was_bust = 0 THEN dt.score_value END), 0.0) as avgScorePerDart,
            COALESCE(AVG(CASE WHEN r.round_number <= 3 THEN dt.score_value END), 0.0) as first9Average,
            COUNT(dt.id) as totalDartsThrown,
            COUNT(CASE WHEN dt.multiplier = 'DOUBLE' THEN 1 END) as doubleHits,
            COUNT(CASE WHEN dt.multiplier = 'TRIPLE' THEN 1 END) as tripleHits,
            COUNT(CASE WHEN dt.field = 0 THEN 1 END) as outOfBounceCount
        FROM dart_throws dt
        JOIN rounds r ON r.id = dt.round_id
        JOIN game_participants gp ON gp.id = r.game_participant_id
        WHERE gp.player_id = :playerId AND dt.is_padding = 0
    """)
    fun getDartStatsForPlayer(playerId: Long): Flow<DartCountRaw>

    @Query("""
        SELECT COALESCE(AVG(round_total), 0.0) FROM (
            SELECT SUM(dt.score_value) as round_total
            FROM rounds r
            JOIN dart_throws dt ON dt.round_id = r.id
            JOIN game_participants gp ON gp.id = r.game_participant_id
            WHERE gp.player_id = :playerId
              AND dt.is_padding = 0
              AND r.is_winning_round = 0
              AND r.was_bust = 0
            GROUP BY r.id
        )
    """)
    fun getAvgScorePerRound(playerId: Long): Flow<Double>

    @Query("""
        SELECT
            COALESCE(MAX(round_total), 0) as highestRound,
            COUNT(CASE WHEN round_total < 10 THEN 1 END) as roundsUnder10,
            COUNT(*) as totalRounds
        FROM (
            SELECT SUM(dt.score_value) as round_total
            FROM rounds r
            JOIN dart_throws dt ON dt.round_id = r.id
            JOIN game_participants gp ON gp.id = r.game_participant_id
            WHERE gp.player_id = :playerId
              AND dt.is_padding = 0
              AND r.was_bust = 0
            GROUP BY r.id
        )
    """)
    fun getRoundStatsForPlayer(playerId: Long): Flow<RoundStatsRaw>
}
