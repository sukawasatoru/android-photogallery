package jp.tinyport.photogallery.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class Converters {
    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime): Long {
        return value.toInstant().toEpochMilli()
    }

    @TypeConverter
    fun intoZonedDateTime(value: Long): ZonedDateTime {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.of("UTC"))
    }
}
