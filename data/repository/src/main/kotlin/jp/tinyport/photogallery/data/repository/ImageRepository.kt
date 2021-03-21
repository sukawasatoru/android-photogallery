package jp.tinyport.photogallery.data.repository

import android.content.Context
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.tinyport.photogallery.data.db.ImageLocalDataSource
import jp.tinyport.photogallery.data.graphql.ImageServerDataSource
import jp.tinyport.photogallery.data.repository.di.RepositoryDispatcher
import jp.tinyport.photogallery.data.repository.di.RepositoryEntryPoint
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ImageRepository {
    suspend fun retrieveImageAndUpdate(): Flow<Result<List<MyImage>, String>>

    suspend fun retrieveImageForPaging(after: String?): Result<Pair<List<MyImage>, String?>, String>
}

class DefaultImageRepository @Inject constructor(
        @ApplicationContext
        private val context: Context,
        @RepositoryDispatcher
        private val dispatcher: CoroutineDispatcher,
        private val remoteDataSource: ImageServerDataSource,
        private val localDataSource: ImageLocalDataSource,
) : ImageRepository {
    private val log = EntryPointAccessors.fromApplication(context, RepositoryEntryPoint::class.java)
            .log()

    override suspend fun retrieveImageAndUpdate():
            Flow<Result<List<MyImage>, String>> {
        return flow {
            when (val data = localDataSource.getImages()) {
                is Ok -> emit(Ok(data.value))
                is Err -> {
                    emit(Err(data.error))
                    return@flow
                }
            }

            val images = mutableListOf<MyImage>()
            var after: String? = null
            while (true) {
                val (retImages, cursor) = when (
                    val data = remoteDataSource.getImages(1000, after)) {
                    is Ok -> {
                        data.value
                    }
                    is Err -> {
                        emit(Err("failed to retrieve image: ${data.error}"))
                        return@flow
                    }
                }
                log.info("@@@@ succeeded")
                images.addAll(retImages)
                if (cursor == null) {
                    break
                }
                after = cursor
            }
            emit(Ok(images))

            when (val data = localDataSource.replaceImages(images)) {
                is Ok -> {
                    // do nothing.
                }
                is Err -> {
                    emit(Err(data.error))
                }
            }
        }.flowOn(dispatcher)
    }

    override suspend fun retrieveImageForPaging(after: String?):
            Result<Pair<List<MyImage>, String?>, String> {
        return when(val data = remoteDataSource.getImages(1000, after)) {
            is Ok -> Ok(data.value)
            is Err -> Err(data.error)
        }
    }
}
