package com.nickstv.app.ui.player

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val streamState by viewModel.streamState.collectAsState()
    val metaState by viewModel.metaState.collectAsState()

    // Kick off both loads
    LaunchedEffect(mediaId, mediaType, season, episode) {
        viewModel.load(mediaType, mediaId, season, episode)
    }

    // Once we have meta, save to continue watching
    LaunchedEffect(metaState, streamState) {
        val meta = (metaState as? UiState.Success)?.data ?: return@LaunchedEffect
        viewModel.markContinueWatching(
            id = mediaId,
            type = mediaType,
            title = meta.displayTitle,
            poster = meta.posterUrl,
            backdrop = meta.backdropUrl,
            year = meta.displayYear,
            season = season,
            episode = episode
        )
    }

    var showOverlay by remember { mutableStateOf(true) }
    var overlayTimer by remember { mutableStateOf(0) }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            overlayTimer = 0
            while (overlayTimer < 40) {
                delay(100)
                overlayTimer++
            }
            showOverlay = false
        }
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = streamState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NicksTheme.AccentRed)
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Playback Error", color = NicksTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = NicksTheme.TextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(20.dp))
                        FocusableButton("← Back", onClick = onBack)
                    }
                }
            }
            is UiState.Success -> {
                val streamUrl = state.data.sources?.firstOrNull()?.url
                if (streamUrl.isNullOrBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No stream available", color = NicksTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(20.dp))
                            FocusableButton("← Back", onClick = onBack)
                        }
                    }
                } else {
                    ExoPlayerView(
                        streamUrl = streamUrl,
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
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NicksTheme.AccentRed)
                }
            }
        }

        // Overlay
        val metaTitle = (metaState as? UiState.Success)?.data?.displayTitle ?: ""
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            PlayerOverlay(
                title = metaTitle,
                season = season,
                episode = episode,
                onBack = onBack,
                onDismiss = { showOverlay = false }
            )
        }
    }
}

@Composable
private fun ExoPlayerView(streamUrl: String, onKeyEvent: (Int) -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Clean up when leaving
    DisposableEffect(Unit) {
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
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        onKeyEvent(keyCode)
                    }
                    false // let ExoPlayer handle playback keys natively
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PlayerOverlay(
    title: String,
    season: Int?,
    episode: Int?,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayBackButton(onClick = onBack)
            Spacer(Modifier.width(16.dp))
            Column {
                if (title.isNotBlank()) {
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (season != null && episode != null) {
                    Text("S${season}E${episode}", color = Color(0xB3FFFFFF), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun OverlayBackButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, tween(150), label = "back_btn")
    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) NicksTheme.AccentRed else Color(0x88000000))
            .border(if (focused) 2.dp else 0.dp, NicksTheme.FocusBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("← Back", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
