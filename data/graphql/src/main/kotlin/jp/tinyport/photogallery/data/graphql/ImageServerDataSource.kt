package jp.tinyport.photogallery.data.graphql

import com.github.michaelbull.result.Result
import jp.tinyport.photogallery.model.MyImage

interface ImageServerDataSource {
    suspend fun getImages(first: Int, after: String?): Result<Pair<List<MyImage>, String?>, String>
}
