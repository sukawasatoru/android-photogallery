package jp.tinyport.photogallery

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.EncodeStrategy
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class PhotoGalleryGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        builder.setDefaultRequestOptions(
                RequestOptions().format(DecodeFormat.PREFER_ARGB_8888)
                        .diskCacheStrategy(CacheStrategy()))
                .setLogLevel(Log.ERROR)
                .setMemoryCache(LruResourceCache(Runtime.getRuntime().maxMemory() / 10))
    }
}

internal class CacheStrategy : DiskCacheStrategy() {
    override fun isDataCacheable(dataSource: DataSource): Boolean {
        return when (dataSource) {
            DataSource.LOCAL -> false
            DataSource.REMOTE -> true
            DataSource.DATA_DISK_CACHE -> false
            DataSource.RESOURCE_DISK_CACHE -> true
            DataSource.MEMORY_CACHE -> false
        }
    }

    override fun isResourceCacheable(
            isFromAlternateCacheKey: Boolean,
            dataSource: DataSource?,
            encodeStrategy: EncodeStrategy?): Boolean {
        return false
    }

    override fun decodeCachedResource(): Boolean {
        return false
    }

    override fun decodeCachedData(): Boolean {
        return true
    }
}
