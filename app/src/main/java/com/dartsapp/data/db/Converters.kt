package com.dartsapp.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLong(value: Long?): Long? = value

    @TypeConverter
    fun toLong(value: Long?): Long? = value
}
