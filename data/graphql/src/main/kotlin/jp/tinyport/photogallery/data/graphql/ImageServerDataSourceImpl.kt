package jp.tinyport.photogallery.data.graphql

import android.Manifest
import androidx.annotation.RequiresPermission
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.toFlow
import com.apollographql.apollo.exception.ApolloHttpException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first

@ExperimentalCoroutinesApi
class ImageServerDataSourceImpl(apiEndpoint: String) : ImageServerDataSource {
    private val apolloClient = ApolloClient.builder()
            .serverUrl(apiEndpoint)
            .build()

    @RequiresPermission(Manifest.permission.INTERNET)
    override suspend fun getImages(first: Int, after: String?):
            Result<Pair<List<MyImage>, String?>, String> {
        return runCatching {
            val response = apolloClient.query(ImageMetaQuery(
                    first = first, after = Input.optional(after)))
                    .toFlow()
                    .first()

            response.errors?.let { errors ->
                // TODO: parse error.
                return@runCatching Err("failed to retrieve data: $errors")
            }

            val dataImages = response.data!!.images!!
            val images = dataImages.nodes.map { data ->
                MyImage(data.url.toString())
            }

            if (dataImages.pageInfo.hasNextPage) {
                Ok(Pair(images, dataImages.pageInfo.endCursor))
            } else {
                Ok(Pair(images, null))
            }
        }.getOrElse {
            it.printStackTrace()
            if (it is ApolloHttpException) {
                return@getOrElse Err("failed to retrieve data2: ${it.rawResponse()}")
            }
            Err("failed to retrieve data2: $it")
        }
    }
}
