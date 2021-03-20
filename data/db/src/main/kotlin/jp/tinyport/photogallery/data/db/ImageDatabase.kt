package jp.tinyport.photogallery.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import jp.tinyport.photogallery.data.db.dao.ImageDao
import jp.tinyport.photogallery.data.db.entity.ImageEntity

@Database(
        entities = [ImageEntity::class],
        version = 1,
)
abstract class ImageDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
}
