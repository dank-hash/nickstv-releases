package com.nickstv.app.ui.browse

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nickstv.app.model.*
import com.nickstv.app.ui.*
import com.nickstv.app.viewmodel.BrowseViewModel

private val MOVIE_GENRES = listOf(
    "All" to null, "Action" to "28", "Adventure" to "12",
    "Comedy" to "35", "Crime" to "80", "Drama" to "18",
    "Fantasy" to "14", "Horror" to "27", "Mystery" to "9648",
    "Romance" to "10749", "Sci-Fi" to "878", "Thriller" to "53"
)
private val TV_GENRES = listOf(
    "All" to null, "Action" to "10759", "Animation" to "16",
    "Comedy" to "35", "Crime" to "80", "Drama" to "18",
    "Family" to "10751", "Mystery" to "9648", "Sci-Fi" to "10765",
    "Thriller" to "53"
)
private val SORT_OPTIONS = listOf(
    "Popularity" to "popularity", "Rating" to "vote_average",
    "Release Date" to "release_date", "Trending" to "trending"
)

@Composable
fun BrowseScreen(
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: BrowseViewModel = viewModel()
) {
    val browseState by viewModel.browseState.collectAsState()
    val allResults by viewModel.allResults.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()

    var filterExpanded by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    // Load more when near end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.discover(loadMore = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(NicksTheme.BgPrimary)) {
        // Header + filter toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Browse", color = NicksTheme.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            FocusableButton(label = if (filterExpanded) "✕ Close Filters" else "⚙ Filters") {
                filterExpanded = !filterExpanded
            }
        }

        // Type selector row
        TypeSelector(
            selected = selectedType,
            onSelect = { viewModel.setType(it) }
        )

        // Filters panel (expandable)
        if (filterExpanded) {
            FiltersPanel(
                selectedType = selectedType,
                selectedGenre = selectedGenre,
                selectedSort = selectedSort,
                onGenreChange = { viewModel.setGenre(it) },
                onSortChange = { viewModel.setSort(it) }
            )
        }

        // Results grid
        when (val state = browseState) {
            is UiState.Loading -> BrowseLoadingGrid()
            is UiState.Error -> ErrorState(state.message) { viewModel.discover() }
            else -> {
                if (allResults.isEmpty() && state is UiState.Success) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results found", color = NicksTheme.TextSecondary, fontSize = 16.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(allResults) { _, item ->
                            MediaCard(item = item, onClick = { onNavigateToDetail(item.mediaType, item.id) })
                        }
                        if (state is UiState.Loading && allResults.isNotEmpty()) {
                            items(6) { ShimmerCard(Modifier.height(210.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeSelector(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        val types = listOf("movie" to "Movies", "tv" to "TV Shows")
        itemsIndexed(types) { _, (type, label) ->
            val isSelected = selected == type
            val interactionSource = remember { MutableInteractionSource() }
            val focused by interactionSource.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NicksTheme.AccentRed else NicksTheme.BgSurface)
                    .border(if (focused) 2.dp else 0.dp, NicksTheme.FocusBorder, RoundedCornerShape(8.dp))
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(type) }
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun FiltersPanel(
    selectedType: String,
    selectedGenre: String?,
    selectedSort: String?,
    onGenreChange: (String?) -> Unit,
    onSortChange: (String?) -> Unit
) {
    val genres = if (selectedType == "tv") TV_GENRES else MOVIE_GENRES
    Column(modifier = Modifier.fillMaxWidth().background(NicksTheme.BgSecondary).padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text("Genre", color = NicksTheme.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(genres) { _, (label, id) ->
                FilterChip(label = label, selected = selectedGenre == id) { onGenreChange(id) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Sort By", color = NicksTheme.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(SORT_OPTIONS) { _, (label, value) ->
                FilterChip(label = label, selected = selectedSort == value) { onSortChange(value) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) NicksTheme.AccentRed else NicksTheme.BgCard)
            .border(if (focused) 2.dp else 0.5.dp, if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.White else NicksTheme.TextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun BrowseLoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(18) { ShimmerCard(Modifier.height(210.dp)) }
    }
}
