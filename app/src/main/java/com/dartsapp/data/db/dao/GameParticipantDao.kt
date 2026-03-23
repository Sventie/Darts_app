package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dartsapp.data.db.entity.GameParticipantEntity
import kotlinx.coroutines.flow.Flow

data class SocialStatsRaw(
    val bestBuddyName: String?,
    val rivalName: String?,
    val easyWinName: String?
)

@Dao
interface GameParticipantDao {
    @Insert
    suspend fun insert(participant: GameParticipantEntity): Long

    @Insert
    suspend fun insertAll(participants: List<GameParticipantEntity>): List<Long>

    @Query("SELECT * FROM game_participants WHERE game_id = :gameId ORDER BY turn_order ASC")
    suspend fun getParticipantsForGame(gameId: Long): List<GameParticipantEntity>

    @Query("UPDATE game_participants SET final_score = :score WHERE id = :participantId")
    suspend fun updateFinalScore(participantId: Long, score: Int)

    @Query("UPDATE game_participants SET placement = :placement WHERE id = :participantId")
    suspend fun updatePlacement(participantId: Long, placement: Int)

    @Query("""
        SELECT player_id FROM game_participants
        WHERE game_id = (SELECT id FROM games ORDER BY started_at DESC LIMIT 1)
        ORDER BY turn_order ASC
    """)
    suspend fun getLastGamePlayerIds(): List<Long>

    @Query("""
        SELECT
            (SELECT p.name
             FROM game_participants gp_self
             JOIN game_participants gp_other
               ON gp_other.game_id = gp_self.game_id AND gp_other.player_id != :playerId
             JOIN games g ON g.id = gp_self.game_id
             JOIN players p ON p.id = gp_other.player_id
             WHERE gp_self.player_id = :playerId AND g.finished_at IS NOT NULL
             GROUP BY gp_other.player_id
             ORDER BY COUNT(*) DESC
             LIMIT 1
            ) as bestBuddyName,
            (SELECT p.name
             FROM game_participants gp_self
             JOIN game_participants gp_other
               ON gp_other.game_id = gp_self.game_id AND gp_other.player_id != :playerId
             JOIN games g ON g.id = gp_self.game_id
             JOIN players p ON p.id = gp_other.player_id
             WHERE gp_self.player_id = :playerId
               AND g.finished_at IS NOT NULL
               AND gp_self.placement IS NOT NULL
               AND gp_other.placement IS NOT NULL
               AND gp_other.placement < gp_self.placement
             GROUP BY gp_other.player_id
             ORDER BY COUNT(*) DESC
             LIMIT 1
            ) as rivalName,
            (SELECT p.name
             FROM game_participants gp_self
             JOIN game_participants gp_other
               ON gp_other.game_id = gp_self.game_id AND gp_other.player_id != :playerId
             JOIN games g ON g.id = gp_self.game_id
             JOIN players p ON p.id = gp_other.player_id
             WHERE gp_self.player_id = :playerId
               AND g.finished_at IS NOT NULL
               AND gp_self.placement IS NOT NULL
               AND gp_other.placement IS NOT NULL
               AND gp_self.placement < gp_other.placement
             GROUP BY gp_other.player_id
             ORDER BY COUNT(*) DESC
             LIMIT 1
            ) as easyWinName
    """)
    fun getSocialStatsForPlayer(playerId: Long): Flow<SocialStatsRaw>
}
