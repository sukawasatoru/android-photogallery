package jp.tinyport.photogallery.model

import java.time.LocalDateTime

data class MyImage(
        val id: String,
        val createdDate: LocalDateTime,
        val url: String,
        val description: String,
) {
    companion object
}
