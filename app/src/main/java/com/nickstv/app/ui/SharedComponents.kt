package com.nickstv.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.nickstv.app.model.*

// ── Theme colours as composable values ───────────────────────────────────────

object NicksTheme {
    val BgPrimary = Color(0xFF0A0A0F)
    val BgSecondary = Color(0xFF12121A)
    val BgCard = Color(0xFF1A1A24)
    val BgSurface = Color(0xFF1E1E2E)
    val AccentRed = Color(0xFFE50914)
    val AccentBlue = Color(0xFF1E90FF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF)
    val TextTertiary = Color(0x66FFFFFF)
    val RatingGold = Color(0xFFFFD700)
    val CardBorder = Color(0xFF2A2A35)
    val FocusBorder = Color(0xFFE50914)
}

// ── Shimmer loading placeholder ───────────────────────────────────────────────

@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1E2E),
            Color(0xFF2A2A3E),
            Color(0xFF1E1E2E),
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(modifier = modifier.background(shimmerBrush, RoundedCornerShape(8.dp)))
}

@Composable
fun ShimmerRail(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ShimmerCard(Modifier.width(120.dp).height(12.dp).padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) {
                ShimmerCard(Modifier.width(140.dp).height(210.dp))
            }
        }
    }
}

// ── Media card ────────────────────────────────────────────────────────────────

@Composable
fun MediaCard(
    item: MediaItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(150),
        label = "card_scale"
    )

    val reqModifier = if (focusRequester != null)
        modifier.width(140.dp).focusRequester(focusRequester)
    else
        modifier.width(140.dp)

    Column(
        modifier = reqModifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(NicksTheme.BgCard)
            .border(
                width = if (focused) 2.dp else 0.5.dp,
                color = if (focused) NicksTheme.FocusBorder else NicksTheme.CardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(210.dp)
        ) {
            AsyncImage(
                model = item.posterUrl.ifBlank { null },
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Type badge
            val typeLabel = when (item.mediaType) {
                "tv" -> "TV"
                "anime" -> "ANIME"
                else -> "MOVIE"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(NicksTheme.AccentRed.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(typeLabel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            // Rating badge
            if (item.displayRating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        "★ ${"%.1f".format(item.displayRating)}",
                        color = NicksTheme.RatingGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = item.displayTitle,
                color = NicksTheme.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.displayYear.isNotBlank()) {
                Text(
                    text = item.displayYear,
                    color = NicksTheme.TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
fun ErrorState(message: String, onRetry: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize().background(NicksTheme.BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Something went wrong",
                color = NicksTheme.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(text = message, color = NicksTheme.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            FocusableButton(label = "Retry", onClick = onRetry)
        }
    }
}

// ── Focusable button ──────────────────────────────────────────────────────────

@Composable
fun FocusableButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(150),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) NicksTheme.AccentRed else NicksTheme.BgSurface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NicksTheme.FocusBorder else NicksTheme.TextTertiary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = NicksTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = NicksTheme.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 24.dp, bottom = 12.dp, top = 20.dp)
    )
}
