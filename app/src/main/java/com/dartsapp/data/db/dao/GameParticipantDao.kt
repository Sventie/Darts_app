package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dartsapp.data.db.entity.GameParticipantEntity

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
}
