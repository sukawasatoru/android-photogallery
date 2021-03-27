package jp.tinyport.photogallery.data.db

import android.content.Context
import androidx.paging.PagingSource
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dagger.hilt.android.EntryPointAccessors
import jp.tinyport.photogallery.data.db.dao.di.DbEntryPoint
import jp.tinyport.photogallery.data.db.entity.ImageEntity
import jp.tinyport.photogallery.model.MyImage

interface ImageLocalDataSource {
    suspend fun getImages(): Result<List<MyImage>, String>

    suspend fun replaceImages(images: List<MyImage>): Result<Unit, String>

    suspend fun saveImages(images: List<MyImage>): Result<Unit, String>

    fun pagingSource(): PagingSource<Int, MyImage>
}

class ImageLocalDataSourceImpl(
        context: Context,
        private val db: ImageDatabase) : ImageLocalDataSource {
    private val log = EntryPointAccessors.fromApplication(context, DbEntryPoint::class.java)
            .log()

    override suspend fun getImages(): Result<List<MyImage>, String> {
        log.debug("[ImageLocalDataSource] getImages")

        return runCatching {
            Ok(db.imageDao().findAll().map(MyImage::from))
        }.getOrElse {
            Err(it.toString())
        }
    }

    override suspend fun replaceImages(images: List<MyImage>): Result<Unit, String> {
        log.debug("[ImageLocalDataSource] replaceImages")

        return runCatching {
            db.imageDao().replaceAll(images.map(MyImage::toEntity))
            Ok(Unit)
        }.getOrElse {
            Err(it.toString())
        }
    }

    override suspend fun saveImages(images: List<MyImage>): Result<Unit, String> {
        log.debug("[ImageLocalDataSource] saveImages")

        return runCatching {
            db.imageDao().saveImages(images.map(MyImage::toEntity))
            Ok(Unit)
        }.getOrElse {
            Err(it.toString())
        }
    }

    override fun pagingSource(): PagingSource<Int, MyImage> {
        log.debug("[ImageLocalDataSource] pagingSource")

        // automatically generated PagingSource uses LimitOffsetDataSource.java.
        // it generates `SELECT * FROM ( " + mSourceQuery.getSql() + " ) LIMIT ? OFFSET ?`
        // so need to create the index for avoiding the table scan.
        return db.imageDao().pagingSource() as PagingSource<Int, MyImage>
    }
}

private fun MyImage.Companion.from(entity: ImageEntity): MyImage {
    return MyImage.new(
            id = entity.id,
            createdDate = entity.createdDate,
            url = entity.url,
            description = entity.description,
    )
}

private fun MyImage.toEntity(): ImageEntity {
    return ImageEntity(
            id = id,
            createdDate = createdDate,
            url = url,
            description = description,
    )
}
