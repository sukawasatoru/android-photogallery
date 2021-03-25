package jp.tinyport.photogallery.data.repository.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.tinyport.photogallery.data.db.ImageDatabase
import jp.tinyport.photogallery.data.db.ImageLocalDataSource
import jp.tinyport.photogallery.data.db.ImageLocalDataSourceImpl
import jp.tinyport.photogallery.data.graphql.ImageServerDataSource
import jp.tinyport.photogallery.data.graphql.ImageServerDataSourceImpl
import jp.tinyport.photogallery.data.repository.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @RepositoryDispatcher
    fun provideDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    fun provideImageDatabase(@ApplicationContext context: Context): ImageDatabase {
        return Room.databaseBuilder(context, ImageDatabase::class.java, "image.db")
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    fun provideImageLocalDataSource(
            @ApplicationContext context: Context,
            db: ImageDatabase): ImageLocalDataSource {
        return ImageLocalDataSourceImpl(context, db)
    }

    @ExperimentalCoroutinesApi
    @Provides
    fun provideImageServerDataSource(): ImageServerDataSource {
        return ImageServerDataSourceImpl(BuildConfig.API_ENDPOINT)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RepositoryDispatcher
