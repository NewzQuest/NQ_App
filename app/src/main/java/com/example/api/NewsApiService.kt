package com.example.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(@Query("lang") lang: String): List<WordPressPost>
}

data class WordPressPost(
    val id: Int,
    val title: RenderedText,
    val excerpt: RenderedText?,
    val content: RenderedText,
    val link: String
)

data class RenderedText(val rendered: String)
