package com.dartsapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.GameDao
import com.dartsapp.data.db.dao.GameParticipantDao
import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.dao.RoundDao
import com.dartsapp.data.db.dao.TrainingDao
import com.dartsapp.data.db.entity.DartThrowEntity
import com.dartsapp.data.db.entity.GameEntity
import com.dartsapp.data.db.entity.GameParticipantEntity
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.db.entity.RoundEntity
import com.dartsapp.data.db.entity.TrainingSessionEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        GameParticipantEntity::class,
        RoundEntity::class,
        DartThrowEntity::class,
        TrainingSessionEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gameParticipantDao(): GameParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun dartThrowDao(): DartThrowDao
    abstract fun trainingDao(): TrainingDao
}
