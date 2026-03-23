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
}
