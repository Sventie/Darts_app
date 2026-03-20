package com.dartsapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dartsapp.data.db.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: Long): GameEntity?

    @Query("SELECT * FROM games ORDER BY started_at DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("UPDATE games SET finished_at = :finishedAt, winner_player_id = :winnerPlayerId WHERE id = :gameId")
    suspend fun finishGame(gameId: Long, finishedAt: Long, winnerPlayerId: Long)
}
