package com.nickstv.app.ui.settings

import android.view.KeyEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nickstv.app.BuildConfig
import com.nickstv.app.model.GitHubRelease
import com.nickstv.app.ui.*
import com.nickstv.app.util.OtaUpdateManager
import com.nickstv.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }

    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is OtaUpdateManager.UpdateState.UpdateAvailable -> {
                latestRelease = s.release
                showUpdateDialog = true
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NicksTheme.BgPrimary)
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = NicksTheme.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        // Version info
        SettingsRow(
            title = "App Version",
            subtitle = BuildConfig.VERSION_NAME,
            clickable = false
        )

        // Check for updates
        SettingsRow(
            title = "Check for Updates",
            subtitle = when (val s = updateState) {
                is OtaUpdateManager.UpdateState.Checking -> "Checking…"
                is OtaUpdateManager.UpdateState.UpToDate -> "You're up to date!"
                is OtaUpdateManager.UpdateState.UpdateAvailable -> "Update available: ${s.release.tagName}"
                is OtaUpdateManager.UpdateState.Downloading -> "Downloading… ${s.progress}%"
                is OtaUpdateManager.UpdateState.ReadyToInstall -> "Ready to install"
                is OtaUpdateManager.UpdateState.Error -> "Error: ${s.message}"
                else -> "Tap to check"
            },
            onClick = { viewModel.checkForUpdates() }
        )

        // Clear cache
        SettingsRow(
            title = "Clear Cache",
            subtitle = "Remove cached images and network responses",
            onClick = {
                runCatching {
                    context.cacheDir?.deleteRecursively()
                    context.externalCacheDir?.deleteRecursively()
                }
            }
        )

        // Placeholder settings
        SettingsRow(title = "Video Quality", subtitle = "Auto (coming soon)", clickable = false)
        SettingsRow(title = "Subtitles", subtitle = "Off (coming soon)", clickable = false)
        SettingsRow(title = "About", subtitle = "Nicks TV — Built with ❤ for Fire TV", clickable = false)
    }

    // Update available dialog
    if (showUpdateDialog) {
        latestRelease?.let { release ->
            UpdateDialog(
                release = release,
                updateState = updateState,
                onDownload = { viewModel.downloadUpdate(release) },
                onDismiss = { showUpdateDialog = false; viewModel.resetUpdateState() }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String = "",
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused && clickable) 1.02f else 1f, tween(150), label = "row")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused && clickable) NicksTheme.BgSurface else NicksTheme.BgCard)
            .border(
                width = if (focused && clickable) 2.dp else 0.5.dp,
                color = if (focused && clickable) NicksTheme.FocusBorder else NicksTheme.CardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (clickable) Modifier
                    .focusable(interactionSource = interactionSource)
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                            (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                             event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                        ) { onClick(); true } else false
                    }
                    .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NicksTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = NicksTheme.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (clickable) {
            Text("›", color = NicksTheme.TextTertiary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun UpdateDialog(
    release: GitHubRelease,
    updateState: OtaUpdateManager.UpdateState,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Update Available — ${release.tagName ?: "New Version"}",
                color = NicksTheme.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    release.body?.take(400) ?: "A new version of Nicks TV is available.",
                    color = NicksTheme.TextSecondary,
                    fontSize = 13.sp
                )
                if (updateState is OtaUpdateManager.UpdateState.Downloading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = updateState.progress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = NicksTheme.AccentRed,
                        trackColor = NicksTheme.CardBorder
                    )
                    Text("Downloading… ${updateState.progress}%", color = NicksTheme.TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (updateState is OtaUpdateManager.UpdateState.ReadyToInstall) {
                    Spacer(Modifier.height(8.dp))
                    Text("✓ Download complete. Install prompt opening…", color = Color(0xFF4CAF50), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            if (updateState is OtaUpdateManager.UpdateState.UpdateAvailable) {
                FocusableButton("Download & Install", onClick = onDownload)
            }
        },
        dismissButton = {
            FocusableButton("Cancel", onClick = onDismiss)
        },
        containerColor = NicksTheme.BgSurface,
        tonalElevation = 8.dp
    )
}
