package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dartsapp.data.db.entity.RoundEntity

@Dao
interface RoundDao {
    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Query("SELECT * FROM rounds WHERE game_participant_id = :participantId ORDER BY round_number ASC")
    suspend fun getRoundsForParticipant(participantId: Long): List<RoundEntity>

    @Query("SELECT COUNT(*) FROM rounds WHERE game_participant_id = :participantId")
    suspend fun getRoundCountForParticipant(participantId: Long): Int

    @Query("DELETE FROM rounds WHERE id = :roundId")
    suspend fun deleteById(roundId: Long)
}
