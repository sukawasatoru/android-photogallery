package jp.tinyport.photogallery.data.db

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String {
        return value.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    @TypeConverter
    fun intoLocalDateTime(value: String): LocalDateTime {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
    }
}
