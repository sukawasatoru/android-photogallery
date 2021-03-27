package jp.tinyport.photogallery.model

import java.time.LocalDateTime

interface MyImage {
    companion object {
        fun new(
                id: String,
                createdDate: LocalDateTime,
                url: String,
                description: String,
        ): MyImage = MyImageImpl(
                id,
                createdDate,
                url,
                description,
        )
    }

    val id: String
    val createdDate: LocalDateTime
    val url: String
    val description: String
}

data class MyImageImpl(
        override val id: String,
        override val createdDate: LocalDateTime,
        override val url: String,
        override val description: String,
) : MyImage {
    companion object
}
