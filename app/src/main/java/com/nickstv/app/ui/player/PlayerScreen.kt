package com.nickstv.app.ui.player

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nickstv.app.model.SourceEntry
import com.nickstv.app.model.UiState
import com.nickstv.app.ui.*
import com.nickstv.app.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    mediaId: String,
    mediaType: String,
    season: Int? = null,
    episode: Int? = null,
    preSelectedUrl: String? = null,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val streamState by viewModel.streamState.collectAsState()
    val metaState by viewModel.metaState.collectAsState()
    val allSources by viewModel.allSources.collectAsState()
    val sourcesLoading by viewModel.sourcesLoading.collectAsState()

    // Active stream URL — starts with pre-selected if supplied, otherwise first from default stream
    var activeUrl by remember { mutableStateOf(preSelectedUrl) }

    LaunchedEffect(mediaId, mediaType, season, episode) {
        viewModel.load(mediaType, mediaId, season, episode)
    }

    // Set initial URL from default stream only if not already set
    LaunchedEffect(streamState) {
        if (activeUrl == null && streamState is UiState.Success) {
            activeUrl = (streamState as UiState.Success).data.sources?.firstOrNull()?.url
        }
    }

    // Save to continue watching once meta is ready
    LaunchedEffect(metaState) {
        val meta = (metaState as? UiState.Success)?.data ?: return@LaunchedEffect
        viewModel.markContinueWatching(
            id = mediaId, type = mediaType,
            title = meta.displayTitle, poster = meta.posterUrl,
            backdrop = meta.backdropUrl, year = meta.displayYear,
            season = season, episode = episode
        )
    }

    var showOverlay by remember { mutableStateOf(true) }
    var showServerPicker by remember { mutableStateOf(false) }
    var overlayTimer by remember { mutableStateOf(0) }

    LaunchedEffect(showOverlay) {
        if (showOverlay && !showServerPicker) {
            overlayTimer = 0
            while (overlayTimer < 50) { delay(100); overlayTimer++ }
            showOverlay = false
        }
    }

    BackHandler {
        when {
            showServerPicker -> showServerPicker = false
            else -> onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── Player ─────────────────────────────────────────────────────────────
        when {
            activeUrl != null -> {
                ExoPlayerView(
                    streamUrl = activeUrl!!,
                    onKeyEvent = { keyCode ->
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                showOverlay = true
                                overlayTimer = 0
                            }
                        }
                    }
                )
            }
            streamState is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NicksTheme.AccentRed)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading stream…", color = NicksTheme.TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            streamState is UiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Stream failed", color = NicksTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text((streamState as UiState.Error).message, color = NicksTheme.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(20.dp))
                        if (allSources.isNotEmpty()) {
                            FocusableButton("Choose Server") { showServerPicker = true; showOverlay = true }
                            Spacer(Modifier.height(10.dp))
                        }
                        FocusableButton("← Back", onClick = onBack)
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NicksTheme.AccentRed)
                }
            }
        }

        // ── Top overlay ────────────────────────────────────────────────────────
        val metaTitle = (metaState as? UiState.Success)?.data?.displayTitle ?: ""
        AnimatedVisibility(
            visible = showOverlay && !showServerPicker,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            PlayerOverlay(
                title = metaTitle,
                season = season,
                episode = episode,
                sourceCount = allSources.size,
                sourcesLoading = sourcesLoading,
                onBack = onBack,
                onDismiss = { showOverlay = false },
                onShowServers = { showServerPicker = true }
            )
        }

        // ── Server picker sheet ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showServerPicker,
            enter = slideInHorizontally(tween(300)) { it },
            exit = slideOutHorizontally(tween(250)) { it }
        ) {
            ServerPickerSheet(
                sources = allSources,
                sourcesLoading = sourcesLoading,
                activeUrl = activeUrl,
                onSelect = { entry ->
                    activeUrl = entry.url
                    showServerPicker = false
                    showOverlay = true
                    overlayTimer = 0
                },
                onDismiss = { showServerPicker = false }
            )
        }
    }
}

// ── ExoPlayer view ────────────────────────────────────────────────────────────

@Composable
private fun ExoPlayerView(streamUrl: String, onKeyEvent: (Int) -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = TrackSelectionParameters.Builder(context)
                .setPreferredVideoMimeTypes("video/avc", "video/mp4v-es")
                .setMaxVideoSize(1920, 1080)
                .build()
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(streamUrl) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) onKeyEvent(keyCode)
                    false
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ── Player overlay ────────────────────────────────────────────────────────────

@Composable
private fun PlayerOverlay(
    title: String,
    season: Int?,
    episode: Int?,
    sourceCount: Int,
    sourcesLoading: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onShowServers: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayButton("← Back", onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (title.isNotBlank()) {
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (season != null && episode != null) {
                    Text("S${season}E${episode}", color = Color(0xB3FFFFFF), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            // Servers button
            val serverLabel = when {
                sourcesLoading && sourceCount == 0 -> "Servers…"
                sourceCount > 0 -> "Servers ($sourceCount)"
                else -> "Servers"
            }
            OverlayButton(serverLabel, onClick = onShowServers)
        }
    }
}

// ── Server picker side sheet ──────────────────────────────────────────────────

@Composable
private fun ServerPickerSheet(
    sources: List<SourceEntry>,
    sourcesLoading: Boolean,
    activeUrl: String?,
    onSelect: (SourceEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dim background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )
        // Sheet
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(380.dp)
                .background(NicksTheme.BgSecondary)
                .padding(vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Choose Server",
                    color = NicksTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (sourcesLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NicksTheme.AccentRed,
                        strokeWidth = 2.dp
                    )
                }
            }

            if (sources.isEmpty() && sourcesLoading) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Scanning servers…", color = NicksTheme.TextSecondary, fontSize = 13.sp)
                }
            } else if (sources.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No servers found", color = NicksTheme.TextSecondary, fontSize = 13.sp)
                }
            } else {
                // Group by providerName
                val grouped = sources.groupBy { it.providerName }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (provider, entries) ->
                        item {
                            Text(
                                provider,
                                color = NicksTheme.AccentRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        itemsIndexed(entries) { _, entry ->
                            ServerRow(
                                entry = entry,
                                isActive = entry.url == activeUrl,
                                onSelect = { onSelect(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerRow(entry: SourceEntry, isActive: Boolean, onSelect: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isActive -> NicksTheme.AccentRed.copy(alpha = 0.2f)
                    focused -> NicksTheme.BgSurface
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused || isActive) 1.5.dp else 0.dp,
                color = if (isActive) NicksTheme.AccentRed else if (focused) NicksTheme.FocusBorder else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onSelect() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.serverLabel,
                color = if (isActive) NicksTheme.AccentRed else NicksTheme.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                entry.quality,
                color = NicksTheme.TextTertiary,
                fontSize = 11.sp
            )
        }
        if (isActive) {
            Text("▶", color = NicksTheme.AccentRed, fontSize = 14.sp)
        }
    }
}

// ── Shared overlay button ─────────────────────────────────────────────────────

@Composable
private fun OverlayButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(150), label = "ovr_btn")
    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) NicksTheme.AccentRed else Color(0x88000000))
            .border(if (focused) 2.dp else 0.dp, NicksTheme.FocusBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
