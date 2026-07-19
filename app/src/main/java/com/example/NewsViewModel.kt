package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NewsViewModel(private val repository: NewsRepository) : ViewModel() {
    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language

    private val _category = MutableStateFlow("General")
    val category: StateFlow<String> = _category

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles: StateFlow<List<NewsArticle>> = _language
        .flatMapLatest { lang ->
            _category.flatMapLatest { cat ->
                repository.getArticlesByLanguageAndCategory(lang, cat)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedArticles: StateFlow<List<NewsArticle>> = repository.getSavedArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh("en")
    }

    fun refresh(lang: String, cat: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.refreshArticles(lang, cat)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load news: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLanguage() {
        val newLang = if (_language.value == "en") "hi" else "en"
        _language.value = newLang
        refresh(newLang, _category.value)
    }

    fun setCategory(category: String) {
        _category.value = category
        refresh(_language.value, category)
    }

    fun toggleSaved(article: NewsArticle) {
        viewModelScope.launch {
            repository.updateSavedStatus(article.id, !article.isSaved)
        }
    }
}
