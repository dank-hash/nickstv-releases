package com.nickstv.app.ui.detail

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import com.nickstv.app.data.local.WatchlistEntity
import com.nickstv.app.model.*
import com.nickstv.app.ui.*
import com.nickstv.app.ui.home.ContentRail
import com.nickstv.app.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    mediaId: String,
    mediaType: String,
    onNavigateToPlayer: (String, String, Int?, Int?, String?) -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val episodeState by viewModel.episodeState.collectAsState()
    val isSaved by viewModel.isSaved(mediaId, mediaType).collectAsState(initial = false)

    LaunchedEffect(mediaId, mediaType) { viewModel.load(mediaId, mediaType) }

    Box(modifier = Modifier.fillMaxSize().background(NicksTheme.BgPrimary)) {
        when (val state = detailState) {
            is UiState.Loading -> DetailLoadingState()
            is UiState.Error -> ErrorState(state.message) { viewModel.load(mediaId, mediaType) }
            is UiState.Success -> {
                val detail = state.data
                DetailContent(
                    detail = detail,
                    mediaId = mediaId,
                    mediaType = mediaType,
                    isSaved = isSaved,
                    selectedSeason = selectedSeason,
                    episodeState = episodeState,
                    onPlayClick = { season, episode ->
                        viewModel.markStarted(mediaId, mediaType, detail.displayTitle,
                            detail.posterUrl, detail.backdropUrl, detail.displayYear, season, episode)
                        onNavigateToPlayer(mediaType, mediaId, season, episode, null)
                    },
                    onToggleWatchlist = {
                        viewModel.toggleWatchlist(
                            WatchlistEntity(
                                id = mediaId, mediaType = mediaType,
                                title = detail.displayTitle, posterUrl = detail.posterUrl,
                                backdropUrl = detail.backdropUrl, year = detail.displayYear,
                                rating = detail.displayRating
                            )
                        )
                    },
                    onSelectSeason = { viewModel.selectSeason(it) },
                    onNavigateToDetail = onNavigateToDetail,
                    onBack = onBack
                )
            }
            else -> DetailLoadingState()
        }
    }
}

@Composable
private fun DetailLoadingState() {
    Column(modifier = Modifier.fillMaxSize()) {
        ShimmerCard(Modifier.fillMaxWidth().height(280.dp))
        Spacer(Modifier.height(20.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
            ShimmerCard(Modifier.width(200.dp).height(28.dp).padding(bottom = 8.dp))
            ShimmerCard(Modifier.width(120.dp).height(14.dp).padding(bottom = 16.dp))
            ShimmerCard(Modifier.fillMaxWidth().height(60.dp))
        }
    }
}

@Composable
private fun DetailContent(
    detail: DetailResponse,
    mediaId: String,
    mediaType: String,
    isSaved: Boolean,
    selectedSeason: Int,
    episodeState: UiState<DetailResponse>,
    onPlayClick: (Int?, Int?) -> Unit,
    onToggleWatchlist: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Backdrop hero
        item {
            BackdropHero(detail = detail, onBack = onBack)
        }

        // Title + meta
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(detail.displayTitle, color = NicksTheme.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                if (!detail.tagline.isNullOrBlank()) {
                    Text(detail.tagline, color = NicksTheme.TextSecondary, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
                MetaRow(detail = detail)
                Spacer(Modifier.height(14.dp))

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusableButton(
                        label = "▶  Watch Now",
                        onClick = {
                            if (mediaType == "tv") onPlayClick(1, 1) else onPlayClick(null, null)
                        }
                    )
                    FocusableButton(
                        label = if (isSaved) "✓ Saved" else "+ Watchlist",
                        onClick = onToggleWatchlist
                    )
                }

                // Overview
                if (!detail.overview.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Overview", color = NicksTheme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(detail.overview, color = NicksTheme.TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }

        // Cast
        detail.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
            item { SectionTitle("Cast") }
            item { CastRail(cast = cast) }
        }

        // TV: Season selector + Episodes
        if (mediaType == "tv" && !detail.seasons.isNullOrEmpty()) {
            item {
                SectionTitle("Seasons & Episodes")
                SeasonSelector(
                    seasons = detail.seasons,
                    selected = selectedSeason,
                    onSelect = onSelectSeason
                )
            }
            item {
                EpisodeList(
                    episodeState = episodeState,
                    onEpisodeClick = { ep -> onPlayClick(selectedSeason, ep) }
                )
            }
        }

        // Recommendations
        detail.recommendations?.takeIf { it.isNotEmpty() }?.let { recs ->
            item { SectionTitle("Recommended For You") }
            item { ContentRail(items = recs, onItemClick = { onNavigateToDetail(it.mediaType, it.id) }) }
        }

        // Similar
        detail.similar?.takeIf { it.isNotEmpty() }?.let { sim ->
            item { SectionTitle("More Like This") }
            item { ContentRail(items = sim, onItemClick = { onNavigateToDetail(it.mediaType, it.id) }) }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun BackdropHero(detail: DetailResponse, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        AsyncImage(
            model = detail.backdropUrl.ifBlank { detail.posterUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(
                    listOf(Color(0x44000000), Color(0xAA0A0A0F), NicksTheme.BgPrimary)
                ))
        )
        // Back button
        BackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (focused) NicksTheme.AccentRed else Color(0x88000000))
            .border(if (focused) 2.dp else 0.dp, NicksTheme.FocusBorder, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetaRow(detail: DetailResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (detail.displayYear.isNotBlank()) {
            Text(detail.displayYear, color = NicksTheme.TextSecondary, fontSize = 13.sp)
        }
        if (detail.displayRating > 0) {
            Text("★ ${"%.1f".format(detail.displayRating)}", color = NicksTheme.RatingGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        detail.runtime?.let { Text("${it}m", color = NicksTheme.TextSecondary, fontSize = 13.sp) }
        detail.numberOfSeasons?.let { Text("$it Season${if (it != 1) "s" else ""}", color = NicksTheme.TextSecondary, fontSize = 13.sp) }
    }
    detail.genreList.takeIf { it.isNotEmpty() }.let { genres ->
        if (!genres.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                genres.take(4).forEach { genre ->
                    GenreChip(genre)
                }
            }
        }
    }
}

@Composable
private fun GenreChip(name: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NicksTheme.BgSurface)
            .border(1.dp, NicksTheme.CardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(name, color = NicksTheme.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun CastRail(cast: List<CastMember>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(cast) { _, member ->
            CastCard(member = member)
        }
    }
}

@Composable
private fun CastCard(member: CastMember) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(150), label = "cast")

    Column(
        modifier = Modifier
            .scale(scale)
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NicksTheme.BgCard)
            .border(if (focused) 2.dp else 0.5.dp, if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder, RoundedCornerShape(8.dp))
            .focusable(interactionSource = interactionSource)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = member.photoUrl.ifBlank { null },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(member.name ?: "", color = NicksTheme.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
        Text(member.character ?: "", color = NicksTheme.TextTertiary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun SeasonSelector(seasons: List<Season>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        itemsIndexed(seasons) { _, season ->
            val isSelected = season.seasonNumber == selected
            val interactionSource = remember { MutableInteractionSource() }
            val focused by interactionSource.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NicksTheme.AccentRed else NicksTheme.BgSurface)
                    .border(if (focused) 2.dp else 0.dp, NicksTheme.FocusBorder, RoundedCornerShape(8.dp))
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(season.seasonNumber) }
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(season.displayName, color = if (isSelected) Color.White else NicksTheme.TextSecondary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun EpisodeList(episodeState: UiState<DetailResponse>, onEpisodeClick: (Int) -> Unit) {
    when (episodeState) {
        is UiState.Loading -> ShimmerCard(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 24.dp))
        is UiState.Success -> {
            val episodes = episodeState.data.episodes ?: emptyList()
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                episodes.take(20).forEach { episode ->
                    EpisodeRow(episode = episode, onClick = { onEpisodeClick(episode.epNumber) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        is UiState.Empty -> {
            // Season not yet loaded — show nothing
        }
        else -> {}
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NicksTheme.BgCard)
            .border(if (focused) 2.dp else 0.5.dp, if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode thumbnail
        AsyncImage(
            model = episode.stillUrl.ifBlank { null },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Ep ${episode.epNumber}  ${episode.displayName}", color = NicksTheme.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            episode.overview?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = NicksTheme.TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                episode.runtime?.let { Text("${it}m", color = NicksTheme.TextTertiary, fontSize = 10.sp) }
                if (episode.displayRating > 0) Text("★ ${"%.1f".format(episode.displayRating)}", color = NicksTheme.RatingGold, fontSize = 10.sp)
            }
        }
        if (focused) {
            Text("▶", color = NicksTheme.AccentRed, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
        }
    }
}
