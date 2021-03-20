package jp.tinyport.photogallery.data.db

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.tinyport.photogallery.data.db.entity.ImageEntity
import jp.tinyport.photogallery.model.MyImage

interface ImageLocalDataSource {
    suspend fun getImages(): Result<List<MyImage>, String>

    suspend fun replaceImages(images: List<MyImage>): Result<Unit, String>
}

class ImageLocalDataSourceImpl(private val db: ImageDatabase) : ImageLocalDataSource {
    override suspend fun getImages(): Result<List<MyImage>, String> {
        return runCatching {
            Ok(db.imageDao().findAll().map(MyImage::from))
        }.getOrElse {
            Err(it.toString())
        }
    }

    override suspend fun replaceImages(images: List<MyImage>): Result<Unit, String> {
        return runCatching {
            db.imageDao().replaceAll(images.map(MyImage::toEntity))
            Ok(Unit)
        }.getOrElse {
            Err(it.toString())
        }
    }
}

private fun MyImage.Companion.from(entity: ImageEntity): MyImage {
    return MyImage(
            id = entity.id,
            url = entity.url,
            description = entity.description,
    )
}

private fun MyImage.toEntity(): ImageEntity {
    return ImageEntity(
            id = id,
            url = url,
            description = description,
    )
}
