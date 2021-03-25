package jp.tinyport.photogallery.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import jp.tinyport.photogallery.data.db.entity.ImageEntity

@Dao
interface ImageDao {
    @Query("delete from image")
    fun deleteAll()

    // TODO: sort
    @Query("select * from image")
    fun findAll(): List<ImageEntity>

    @Query("SELECT * FROM image ORDER BY createdDate")
    fun pagingSource(): PagingSource<Int, ImageEntity>

    @Insert
    fun saveImages(entities: List<ImageEntity>)

    @Transaction
    fun replaceAll(images: List<ImageEntity>) {
        deleteAll()
        saveImages(images)
    }
}
