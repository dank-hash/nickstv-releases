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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val rdTokenInput by viewModel.rdTokenInput.collectAsState()
    val rdValidationState by viewModel.rdValidationState.collectAsState()
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var showRdToken by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = NicksTheme.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        // ── Real-Debrid ────────────────────────────────────────────────────────
        Text(
            "Real-Debrid",
            color = NicksTheme.AccentRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        RealDebridSection(
            tokenInput = rdTokenInput,
            validationState = rdValidationState,
            showToken = showRdToken,
            onToggleShow = { showRdToken = !showRdToken },
            onTokenChange = viewModel::updateRdTokenInput,
            onValidate = viewModel::saveAndValidateRdToken,
            onClear = viewModel::clearRdToken
        )

        Spacer(Modifier.height(8.dp))

        // ── App ───────────────────────────────────────────────────────────────
        Text(
            "App",
            color = NicksTheme.AccentRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

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

@Composable
private fun RealDebridSection(
    tokenInput: String,
    validationState: SettingsViewModel.RdValidationState,
    showToken: Boolean,
    onToggleShow: () -> Unit,
    onTokenChange: (String) -> Unit,
    onValidate: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NicksTheme.BgCard)
            .border(0.5.dp, NicksTheme.CardBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status banner
        val (statusText, statusColor) = when (validationState) {
            is SettingsViewModel.RdValidationState.Idle ->
                Pair("No token set — Real-Debrid sources will not appear", NicksTheme.TextTertiary)
            is SettingsViewModel.RdValidationState.Validating ->
                Pair("Validating…", NicksTheme.TextSecondary)
            is SettingsViewModel.RdValidationState.Saved ->
                Pair("Token saved — tap Validate to confirm account", NicksTheme.RatingGold)
            is SettingsViewModel.RdValidationState.Valid ->
                Pair("✓ Connected: ${validationState.username}", Color(0xFF4CAF50))
            is SettingsViewModel.RdValidationState.Error ->
                Pair("✗ ${validationState.message}", NicksTheme.AccentRed)
        }
        Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        // Token input field
        OutlinedTextField(
            value = tokenInput,
            onValueChange = onTokenChange,
            label = { Text("Real-Debrid API Token", color = NicksTheme.TextSecondary, fontSize = 12.sp) },
            placeholder = { Text("Paste token here…", color = NicksTheme.TextTertiary, fontSize = 13.sp) },
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val interactionSource = remember { MutableInteractionSource() }
                val focused by interactionSource.collectIsFocusedAsState()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (focused) NicksTheme.BgSurface else Color.Transparent)
                        .border(if (focused) 1.dp else 0.dp, NicksTheme.FocusBorder, RoundedCornerShape(6.dp))
                        .clickable(interactionSource = interactionSource, indication = null) { onToggleShow() }
                        .focusable(interactionSource = interactionSource)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (showToken) "Hide" else "Show", color = NicksTheme.TextSecondary, fontSize = 11.sp)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NicksTheme.FocusBorder,
                unfocusedBorderColor = NicksTheme.CardBorder,
                focusedTextColor = NicksTheme.TextPrimary,
                unfocusedTextColor = NicksTheme.TextPrimary,
                cursorColor = NicksTheme.AccentRed
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FocusableButton(
                label = if (validationState is SettingsViewModel.RdValidationState.Validating) "Validating…" else "Save & Validate",
                onClick = onValidate
            )
            if (tokenInput.isNotBlank() || validationState !is SettingsViewModel.RdValidationState.Idle) {
                FocusableButton(label = "Clear", onClick = onClear)
            }
        }

        // Help text
        Text(
            "Get your API token at real-debrid.com/apitoken",
            color = NicksTheme.TextTertiary,
            fontSize = 10.sp
        )
    }
}

