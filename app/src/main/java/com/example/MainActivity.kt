package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DatabaseProvider
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        
        val db = DatabaseProvider.getDatabase(this)
        val repository = NewsRepository(db.newsDao())
        val viewModel = NewsViewModel(repository)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NewsApp(viewModel, tts)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewsApp(viewModel: NewsViewModel, tts: TextToSpeech) {
    val articles by viewModel.articles.collectAsState()
    val language by viewModel.language.collectAsState()
    val category by viewModel.category.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pagerState = rememberPagerState(pageCount = { articles.size })
    val context = LocalContext.current
    var darkTheme by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    MyApplicationTheme(darkTheme = darkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_newzquest_logo),
                            contentDescription = "NewzQuest Logo",
                            modifier = Modifier.size(56.dp)
                        )
                    },
                    actions = {
                        TextButton(onClick = { viewModel.toggleLanguage() }) {
                            Text(language.uppercase())
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (darkTheme) "Light Mode" else "Dark Mode") },
                                onClick = { darkTheme = !darkTheme; showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Category: $category") },
                                onClick = { /* Show category picker or just cycle */ viewModel.setCategory(if (category == "General") "Technology" else "General"); showMenu = false }
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("No articles found")
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) { page ->
                    val article = articles[page]
                    NewsArticleItem(
                        article,
                        onSaveClick = { viewModel.toggleSaved(article) },
                        onTtsClick = { tts.speak(article.title + ". " + article.excerpt, TextToSpeech.QUEUE_FLUSH, null, null) },
                        onShareClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, article.title)
                                putExtra(android.content.Intent.EXTRA_TEXT, article.title + "\n\n" + article.link)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NewsArticleItem(
    article: NewsArticle, 
    onSaveClick: () -> Unit,
    onTtsClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (article.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = article.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                if (article.excerpt.isNotEmpty()) {
                    Text(text = article.excerpt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            imageVector = if (article.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (article.isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onTtsClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "TTS", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

