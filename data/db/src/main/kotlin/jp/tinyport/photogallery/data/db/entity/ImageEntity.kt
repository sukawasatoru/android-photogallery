package jp.tinyport.photogallery.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image")
data class ImageEntity(
        @PrimaryKey
        val id: String,
        val url: String,
        val description: String,
)
