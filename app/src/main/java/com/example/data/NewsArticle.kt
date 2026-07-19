package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val excerpt: String = "",
    val content: String,
    val language: String, // "en" or "hi"
    val category: String, // "Politics", "Technology", etc.
    val isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val link: String = "",
    val imageUrl: String = ""
)
