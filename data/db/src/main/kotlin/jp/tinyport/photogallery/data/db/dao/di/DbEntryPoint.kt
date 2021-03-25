package jp.tinyport.photogallery.data.db.dao.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.tinyport.logger.Logger

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DbEntryPoint {
    fun log(): Logger
}
