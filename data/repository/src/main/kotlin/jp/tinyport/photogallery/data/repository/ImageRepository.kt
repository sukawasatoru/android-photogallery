package jp.tinyport.photogallery.data.repository

import com.github.michaelbull.result.Result
import jp.tinyport.photogallery.data.graphql.ImageServerDataSource
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class ImageRepository(
        private val dispatcher: CoroutineDispatcher,
        private val dataSource: ImageServerDataSource) {
    suspend fun retrieveImage(first: Int, after: String?):
            Result<Pair<List<MyImage>, String?>, String> = withContext(dispatcher) {
        dataSource.getImages(first, after)
    }
}
