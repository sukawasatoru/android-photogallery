package jp.tinyport.photogallery.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import jp.tinyport.photogallery.data.db.dao.ImageDao
import jp.tinyport.photogallery.data.db.entity.ImageEntity

@Database(
        entities = [ImageEntity::class],
        version = 1,
)
@TypeConverters(Converters::class)
abstract class ImageDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
}
