package com.nickstv.app.ui.home

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nickstv.app.data.local.ContinueWatchingEntity
import com.nickstv.app.data.local.WatchlistEntity
import com.nickstv.app.model.*
import com.nickstv.app.ui.*
import com.nickstv.app.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val homeState by viewModel.homeState.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(NicksTheme.BgPrimary)) {
        when (val state = homeState) {
            is UiState.Loading -> HomeLoadingState()
            is UiState.Error -> ErrorState(state.message) { viewModel.loadHome() }
            is UiState.Success -> HomeContent(
                home = state.data,
                watchlist = watchlist,
                continueWatching = continueWatching,
                onNavigateToDetail = onNavigateToDetail
            )
            else -> HomeLoadingState()
        }
    }
}

@Composable
private fun HomeLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ShimmerCard(Modifier.fillMaxWidth().height(300.dp))
            Spacer(Modifier.height(24.dp))
        }
        repeat(3) {
            item { ShimmerRail(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
        }
    }
}

@Composable
private fun HomeContent(
    home: HomeResponse,
    watchlist: List<WatchlistEntity>,
    continueWatching: List<ContinueWatchingEntity>,
    onNavigateToDetail: (String, String) -> Unit
) {
    val bannerItems = buildList {
        home.trendingMovies?.take(5)?.forEach { add(it) }
        home.popularTv?.take(3)?.forEach { add(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hero banner
        if (bannerItems.isNotEmpty()) {
            item {
                HeroBanner(items = bannerItems, onItemClick = { item ->
                    onNavigateToDetail(item.mediaType, item.id)
                })
            }
        }

        // Continue Watching
        if (continueWatching.isNotEmpty()) {
            item {
                SectionTitle("Continue Watching")
                ContinueWatchingRail(items = continueWatching, onItemClick = { item ->
                    onNavigateToDetail(item.mediaType, item.id)
                })
            }
        }

        // Watchlist rail
        if (watchlist.isNotEmpty()) {
            item {
                SectionTitle("My Watchlist")
                ContentRail(
                    items = watchlist.map { w ->
                        MediaItem(
                            id = w.id, title = w.title, name = null, overview = null,
                            image = w.posterUrl, poster = w.posterUrl, backdrop = w.backdropUrl,
                            year = w.year, rating = w.rating.toString(), type = w.mediaType,
                            genres = null, success = true
                        )
                    },
                    onItemClick = { item -> onNavigateToDetail(item.mediaType, item.id) }
                )
            }
        }

        // Trending Movies
        home.trendingMovies?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Trending Movies")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // Trending TV
        home.popularTv?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Trending TV Shows")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // New Releases
        home.newReleases?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("New Releases")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // Popular Movies
        home.popularMovies?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Popular Movies")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // Action Movies
        home.actionMovies?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Action & Adventure")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // Sci-Fi
        home.scifiMovies?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Sci-Fi")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // Thriller
        home.thrillerMovies?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Thriller")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }

        // Comedy
        home.comedyMovies?.takeIf { it.isNotEmpty() }?.let { items ->
            item {
                SectionTitle("Comedy")
                ContentRail(items = items, onItemClick = { onNavigateToDetail(it.mediaType, it.id) })
            }
        }
    }
}

// ── Hero Banner ────────────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    var userInteracted by remember { mutableStateOf(false) }

    // Auto-scroll every 6 seconds unless user is interacting
    LaunchedEffect(userInteracted) {
        if (!userInteracted) {
            while (true) {
                delay(6000L)
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }

    val item = items[currentIndex]
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(focused) { userInteracted = focused }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {
                onItemClick(item)
            }
    ) {
        // Backdrop
        AnimatedContent(targetState = item, transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) }, label = "hero") { target ->
            AsyncImage(
                model = target.backdropUrl.ifBlank { target.posterUrl },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC0A0A0F), NicksTheme.BgPrimary),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 40.dp, vertical = 28.dp)
        ) {
            Text(
                text = item.displayTitle,
                color = NicksTheme.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.displayYear.isNotBlank()) {
                    Text(item.displayYear, color = NicksTheme.TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.width(12.dp))
                }
                if (item.displayRating > 0) {
                    Text(
                        "★ ${"%.1f".format(item.displayRating)}",
                        color = NicksTheme.RatingGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!item.overview.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.overview,
                    color = NicksTheme.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            Row {
                FocusableButton("▶  Watch Now", onClick = { onItemClick(item) })
            }
        }

        // Page indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { idx, _ ->
                Box(
                    modifier = Modifier
                        .size(if (idx == currentIndex) 10.dp else 6.dp)
                        .background(
                            if (idx == currentIndex) NicksTheme.AccentRed else NicksTheme.TextTertiary,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        // Focus ring
        if (focused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, NicksTheme.FocusBorder)
            )
        }
    }
}

// ── Horizontal content rail ────────────────────────────────────────────────────

@Composable
fun ContentRail(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items) { _, item ->
            MediaCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

// ── Continue watching rail ────────────────────────────────────────────────────

@Composable
private fun ContinueWatchingRail(items: List<ContinueWatchingEntity>, onItemClick: (ContinueWatchingEntity) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items) { _, item ->
            ContinueCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun ContinueCard(item: ContinueWatchingEntity, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(150), label = "cont")

    Column(
        modifier = Modifier
            .scale(scale)
            .width(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NicksTheme.BgCard)
            .border(if (focused) 2.dp else 0.5.dp, if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
            AsyncImage(model = item.backdropUrl.ifBlank { item.posterUrl }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))))
            item.season?.let { s ->
                item.episode?.let { e ->
                    Text("S${s}E${e}", Modifier.align(Alignment.BottomStart).padding(8.dp), color = NicksTheme.TextSecondary, fontSize = 10.sp)
                }
            }
        }
        Text(item.title, Modifier.padding(8.dp), color = NicksTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
