package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dartsapp.data.db.entity.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {

    @Insert
    suspend fun insert(session: TrainingSessionEntity): Long

    @Query("""
        SELECT * FROM training_sessions
        WHERE player_id = :playerId
        ORDER BY completed_at DESC
        LIMIT :limit
    """)
    fun getRecentByPlayer(playerId: Long, limit: Int = 10): Flow<List<TrainingSessionEntity>>

    @Query("""
        SELECT * FROM training_sessions
        WHERE player_id = :playerId AND mode = :mode
        ORDER BY completed_at DESC
        LIMIT :limit
    """)
    fun getRecentByPlayerAndMode(
        playerId: Long,
        mode: String,
        limit: Int = 5
    ): Flow<List<TrainingSessionEntity>>
}
