package jp.tinyport.photogallery.data.repository

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
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

    suspend fun retrieveImageForPaging(loadSize: Int, after: String?): Result<Pair<List<MyImage>, String?>, String>

    fun imageStream(): Flow<PagingData<MyImage>>
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

    override suspend fun retrieveImageForPaging(loadSize: Int, after: String?):
            Result<Pair<List<MyImage>, String?>, String> {
        return when (val data = remoteDataSource.getImages(loadSize, after)) {
            is Ok -> Ok(data.value)
            is Err -> Err(data.error)
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun imageStream(): Flow<PagingData<MyImage>> {
        return Pager(
                config = PagingConfig(
                        pageSize = 1000,
                        maxSize = 5000,
                ),
                remoteMediator = object : RemoteMediator<Int, MyImage>() {
                    val nextKeys = mutableSetOf<String>()

                    override suspend fun load(
                            loadType: LoadType,
                            state: PagingState<Int, MyImage>): MediatorResult {
                        val after = when (loadType) {
                            LoadType.REFRESH -> {
                                log.info("[ImageRepository] loadType: %s, pageSize: %s",
                                        loadType.name, state.config.pageSize)
                                null
                            }
                            LoadType.PREPEND -> {
                                log.info("[ImageRepository] loadType: %s", loadType.name)
                                return MediatorResult.Success(true)
                            }
                            LoadType.APPEND -> {
                                log.info("[ImageRepository] loadType: %s, pageSize: %s, nextKeys: %s",
                                        loadType.name, state.config.pageSize, nextKeys.lastOrNull())
                                if (state.lastItemOrNull() == null) {
                                    return MediatorResult.Success(false)
                                }
                                nextKeys.lastOrNull()
                            }
                        }
                        val (data, nextKey) = when (
                            val data = remoteDataSource.getImages(state.config.pageSize, after)) {
                            is Ok -> data.value
                            is Err -> return MediatorResult.Error(Exception(data.error))
                        }

                        if (loadType == LoadType.REFRESH) {
                            localDataSource.replaceImages(data)
                        } else {
                            localDataSource.saveImages(data)
                        }

                        nextKey?.let {
                            nextKeys.add(it)
                        }

                        return MediatorResult.Success(nextKey == null)
                    }
                },
                pagingSourceFactory = { localDataSource.pagingSource() }
        )
                .flow
                .flowOn(dispatcher)
    }
}
