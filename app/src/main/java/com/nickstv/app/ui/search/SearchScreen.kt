package com.nickstv.app.ui.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nickstv.app.model.*
import com.nickstv.app.ui.*
import com.nickstv.app.viewmodel.SearchViewModel
import java.util.Locale

@Composable
fun SearchScreen(
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchState by viewModel.searchState.collectAsState()
    val query by viewModel.query.collectAsState()
    val queryInputFocus = remember { FocusRequester() }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { recognizedText ->
                viewModel.updateQuery(recognizedText)
                viewModel.search(recognizedText)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NicksTheme.BgPrimary)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Header
        Text(
            "Search",
            color = NicksTheme.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                onSearch = { viewModel.search(it) },
                modifier = Modifier.weight(1f),
                focusRequester = queryInputFocus
            )
            Spacer(Modifier.width(12.dp))
            VoiceButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for movies and shows…")
                    }
                    runCatching { voiceLauncher.launch(intent) }
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Results
        when (val state = searchState) {
            is UiState.Loading -> SearchLoadingGrid()
            is UiState.Error -> ErrorState(state.message) { viewModel.search(query) }
            is UiState.Empty -> SearchEmptyState(query)
            is UiState.Success -> {
                val items = state.data.results ?: emptyList()
                if (items.isEmpty()) {
                    SearchEmptyState(query)
                } else {
                    SearchResultsGrid(items = items, onItemClick = { item ->
                        onNavigateToDetail(item.mediaType, item.id)
                    })
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { queryInputFocus.requestFocus() }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NicksTheme.BgSurface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = NicksTheme.TextPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(NicksTheme.AccentRed),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("Search movies, shows…", color = NicksTheme.TextTertiary, fontSize = 15.sp)
                }
                inner()
            }
        )
    }
}

@Composable
private fun VoiceButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, tween(150), label = "voice")

    Box(
        modifier = Modifier
            .scale(scale)
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) NicksTheme.AccentRed else NicksTheme.BgSurface)
            .border(if (focused) 2.dp else 1.dp, if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text("🎤", fontSize = 18.sp)
    }
}

@Composable
private fun SearchLoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(12) {
            ShimmerCard(Modifier.height(210.dp))
        }
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            if (query.isBlank()) {
                Text("Search for movies and TV shows", color = NicksTheme.TextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center)
                Text("Use the remote keyboard or voice search", color = NicksTheme.TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center)
            } else {
                Text("No results for \"$query\"", color = NicksTheme.TextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center)
                Text("Try a different search term", color = NicksTheme.TextTertiary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SearchResultsGrid(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items) { _, item ->
            MediaCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

