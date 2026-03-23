package com.dartsapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dartsapp.data.db.dao.DartThrowDao
import com.dartsapp.data.db.dao.GameDao
import com.dartsapp.data.db.dao.GameParticipantDao
import com.dartsapp.data.db.dao.PlayerDao
import com.dartsapp.data.db.dao.RoundDao
import com.dartsapp.data.db.entity.DartThrowEntity
import com.dartsapp.data.db.entity.GameEntity
import com.dartsapp.data.db.entity.GameParticipantEntity
import com.dartsapp.data.db.entity.PlayerEntity
import com.dartsapp.data.db.entity.RoundEntity

@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        GameParticipantEntity::class,
        RoundEntity::class,
        DartThrowEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gameParticipantDao(): GameParticipantDao
    abstract fun roundDao(): RoundDao
    abstract fun dartThrowDao(): DartThrowDao
}
