package jp.tinyport.photogallery.data.graphql

import android.Manifest
import androidx.annotation.RequiresPermission
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.toFlow
import com.apollographql.apollo.exception.ApolloHttpException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.tinyport.photogallery.data.graphql.type.CustomType
import jp.tinyport.photogallery.model.MyImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface ImageServerDataSource {
    suspend fun getImages(first: Int, after: String?): Result<Pair<List<MyImage>, String?>, String>
}

@ExperimentalCoroutinesApi
class ImageServerDataSourceImpl(apiEndpoint: String) : ImageServerDataSource {
    private val apolloClient = ApolloClient.builder()
            .serverUrl(apiEndpoint)
            .addCustomTypeAdapter(CustomType.DATETIMEUTC, object : CustomTypeAdapter<LocalDateTime> {
                override fun decode(value: CustomTypeValue<*>): LocalDateTime {

                    return LocalDateTime.parse(value.value.toString(), DateTimeFormatter.ISO_DATE_TIME)
                }

                override fun encode(value: LocalDateTime): CustomTypeValue<*> {
                    return CustomTypeValue.GraphQLString(value.format(DateTimeFormatter.ISO_DATE_TIME))
                }
            })
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
            val images = dataImages.nodes.map(MyImage::from)

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

private fun MyImage.Companion.from(entity: ImageMetaQuery.Node): MyImage {
    return new(
            id = entity.id,
            createdDate = entity.createdDate,
            url = entity.url,
            description = entity.description,
    )
}
