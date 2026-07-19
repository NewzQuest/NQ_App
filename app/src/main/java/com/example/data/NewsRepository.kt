package com.example.data

import com.example.api.NetworkProvider
import kotlinx.coroutines.flow.Flow
import com.example.BuildConfig

class NewsRepository(private val newsDao: NewsDao) {
    fun getArticlesByLanguage(language: String): Flow<List<NewsArticle>> = newsDao.getArticlesByLanguage(language)

    suspend fun refreshArticles(language: String) {
        try {
            val apiPosts = NetworkProvider.retrofit.getPosts(lang = language)
            val articles = apiPosts.map { 
                NewsArticle(
                    title = it.title.rendered, 
                    excerpt = it.excerpt?.rendered ?: "",
                    content = it.content.rendered, 
                    language = language, 
                    category = "General",
                    link = it.link
                ) 
            }
            newsDao.insertArticles(articles)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun getSavedArticles(): Flow<List<NewsArticle>> = newsDao.getSavedArticles()

    suspend fun updateSavedStatus(id: Int, isSaved: Boolean) = newsDao.updateSavedStatus(id, isSaved)

    suspend fun insertArticles(articles: List<NewsArticle>) = newsDao.insertArticles(articles)
}
