package jp.tinyport.photogallery.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import jp.tinyport.photogallery.model.MyImage
import java.time.ZonedDateTime

@Entity(tableName = "image")
data class ImageEntity(
        @PrimaryKey
        override val id: String,
        @ColumnInfo(index = true)
        override val createdDate: ZonedDateTime,
        override val url: String,
        override val description: String,
) : MyImage {
    companion object
}
