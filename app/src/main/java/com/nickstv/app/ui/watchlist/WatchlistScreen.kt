package com.nickstv.app.ui.watchlist

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
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
import com.nickstv.app.data.local.WatchlistEntity
import com.nickstv.app.model.MediaItem
import com.nickstv.app.ui.*
import com.nickstv.app.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: WatchlistViewModel = viewModel()
) {
    val watchlist by viewModel.watchlist.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(NicksTheme.BgPrimary).padding(top = 20.dp)) {
        Text(
            "My Watchlist",
            color = NicksTheme.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (watchlist.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔖", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Your watchlist is empty", color = NicksTheme.TextSecondary, fontSize = 16.sp)
                    Text("Add movies and shows from their detail pages", color = NicksTheme.TextTertiary, fontSize = 13.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(watchlist) { _, entity ->
                    WatchlistCard(
                        entity = entity,
                        onClick = { onNavigateToDetail(entity.mediaType, entity.id) },
                        onRemove = { viewModel.remove(entity.id, entity.mediaType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistCard(
    entity: WatchlistEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(150), label = "wl_card")
    var showRemove by remember { mutableStateOf(false) }

    LaunchedEffect(focused) { showRemove = focused }

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(NicksTheme.BgCard)
            .border(if (focused) 2.dp else 0.5.dp, if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .width(140.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
            AsyncImage(
                model = entity.posterUrl.ifBlank { null },
                contentDescription = entity.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    .background(NicksTheme.AccentRed.copy(0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(if (entity.mediaType == "tv") "TV" else "MOVIE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(entity.title, color = NicksTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (entity.year.isNotBlank()) Text(entity.year, color = NicksTheme.TextTertiary, fontSize = 10.sp)
        }
    }
}
