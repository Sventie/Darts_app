package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dartsapp.data.db.entity.TrainingThrowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingThrowDao {

    @Insert
    suspend fun insert(throw_: TrainingThrowEntity)

    @Query("SELECT * FROM training_throws WHERE player_id = :playerId")
    fun getForPlayer(playerId: Long): Flow<List<TrainingThrowEntity>>

    @Query("SELECT COUNT(*) FROM training_throws WHERE player_id = :playerId")
    fun getCountForPlayer(playerId: Long): Flow<Int>

    // fromOffset = fromRank - 2  (negative means no lower bound)
    // toOffset   = toRank - 1
    // Uses COUNT-based row positioning to avoid bound params in LIMIT/OFFSET.
    @Query("""
        SELECT t.* FROM training_throws t
        WHERE t.player_id = :playerId
        AND (:fromOffset < 0 OR t.thrown_at > (
            SELECT s.completed_at FROM training_sessions s
            WHERE s.player_id = :playerId
            AND (SELECT COUNT(*) FROM training_sessions s2
                 WHERE s2.player_id = :playerId
                 AND s2.completed_at < s.completed_at) = :fromOffset
        ))
        AND t.thrown_at <= (
            SELECT s.completed_at FROM training_sessions s
            WHERE s.player_id = :playerId
            AND (SELECT COUNT(*) FROM training_sessions s2
                 WHERE s2.player_id = :playerId
                 AND s2.completed_at < s.completed_at) = :toOffset
        )
    """)
    fun getForPlayerInSessionRange(
        playerId: Long,
        fromOffset: Int,
        toOffset: Int
    ): Flow<List<TrainingThrowEntity>>

    @Query("""
        SELECT COUNT(*) FROM training_throws t
        WHERE t.player_id = :playerId
        AND (:fromOffset < 0 OR t.thrown_at > (
            SELECT s.completed_at FROM training_sessions s
            WHERE s.player_id = :playerId
            AND (SELECT COUNT(*) FROM training_sessions s2
                 WHERE s2.player_id = :playerId
                 AND s2.completed_at < s.completed_at) = :fromOffset
        ))
        AND t.thrown_at <= (
            SELECT s.completed_at FROM training_sessions s
            WHERE s.player_id = :playerId
            AND (SELECT COUNT(*) FROM training_sessions s2
                 WHERE s2.player_id = :playerId
                 AND s2.completed_at < s.completed_at) = :toOffset
        )
    """)
    fun getCountForPlayerInSessionRange(
        playerId: Long,
        fromOffset: Int,
        toOffset: Int
    ): Flow<Int>
}
