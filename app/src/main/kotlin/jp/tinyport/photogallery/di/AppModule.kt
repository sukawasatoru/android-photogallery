package jp.tinyport.photogallery.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.tinyport.photogallery.data.graphql.ImageServerDataSource
import jp.tinyport.photogallery.data.graphql.ImageServerDataSourceImpl
import jp.tinyport.photogallery.data.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Suppress("EXPERIMENTAL_API_USAGE")
    @Provides
    fun provideImageServerDataSource(): ImageServerDataSource {
        return ImageServerDataSourceImpl()
    }

    @Provides
    @Singleton
    fun provideImageRepository(dataSource: ImageServerDataSource): ImageRepository {
        return ImageRepository(Dispatchers.IO, dataSource)
    }
}
