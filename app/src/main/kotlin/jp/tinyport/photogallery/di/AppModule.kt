package jp.tinyport.photogallery.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.tinyport.logger.Logger
import jp.tinyport.photogallery.data.repository.DefaultImageRepository
import jp.tinyport.photogallery.data.repository.ImageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideImageRepository(repo: DefaultImageRepository): ImageRepository {
        return repo
    }

    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return Logger()
    }
}
