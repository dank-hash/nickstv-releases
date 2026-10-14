package com.nickstv.app.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nickstv.app.BuildConfig
import com.nickstv.app.NicksTVApp
import com.nickstv.app.model.GitHubRelease
import com.nickstv.app.model.UiState
import com.nickstv.app.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class OtaUpdateManager(
    private val context: Context = NicksTVApp.instance,
    private val repo: UpdateRepository = NicksTVApp.instance.updateRepository
) {
    private val token = BuildConfig.GITHUB_TOKEN
    private val owner = BuildConfig.GITHUB_REPO_OWNER
    private val repoName = BuildConfig.GITHUB_REPO_NAME

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private var downloadId: Long = -1L

    sealed class UpdateState {
        object Idle : UpdateState()
        object Checking : UpdateState()
        object UpToDate : UpdateState()
        data class UpdateAvailable(val release: GitHubRelease) : UpdateState()
        data class Downloading(val progress: Int) : UpdateState()
        object ReadyToInstall : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    suspend fun checkForUpdates() {
        _updateState.value = UpdateState.Checking
        when (val result = repo.getLatestRelease(owner, repoName, token)) {
            is UiState.Success -> {
                val release = result.data
                val latestVersion = release.tagName?.trimStart('v', 'V') ?: ""
                val currentVersion = BuildConfig.VERSION_NAME
                if (isNewerVersion(latestVersion, currentVersion)) {
                    _updateState.value = UpdateState.UpdateAvailable(release)
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            }
            is UiState.Error -> {
                _updateState.value = UpdateState.Error(result.message)
            }
            else -> {}
        }
    }

    fun downloadUpdate(release: GitHubRelease) {
        val apkAsset = release.assets?.firstOrNull { it.name?.endsWith(".apk") == true }
            ?: run { _updateState.value = UpdateState.Error("No APK asset found in release"); return }
        val downloadUrl = apkAsset.downloadUrl ?: run { _updateState.value = UpdateState.Error("Invalid download URL"); return }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val destFile = File(context.externalCacheDir, "nickstv-latest.apk")
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Nicks TV Update")
            setDescription("Downloading ${release.tagName ?: "update"}…")
            setMimeType("application/vnd.android.package-archive")
            setDestinationUri(Uri.fromFile(destFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        }
        downloadId = downloadManager.enqueue(request)
        _updateState.value = UpdateState.Downloading(0)

        // Poll progress
        val progressThread = Thread {
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else 0
                    val bytes = if (bytesIdx >= 0) cursor.getLong(bytesIdx) else 0L
                    val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else 1L
                    val progress = if (total > 0) ((bytes * 100) / total).toInt() else 0
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> _updateState.value = UpdateState.Downloading(progress)
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _updateState.value = UpdateState.ReadyToInstall
                            cursor.close()
                            installApk(destFile)
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            _updateState.value = UpdateState.Error("Download failed")
                            cursor.close()
                            break
                        }
                    }
                }
                cursor.close()
                Thread.sleep(500)
            }
        }
        progressThread.isDaemon = true
        progressThread.start()
    }

    private fun installApk(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            _updateState.value = UpdateState.Error("Install failed: ${it.message}")
        }
    }

    fun reset() { _updateState.value = UpdateState.Idle }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) { false }
    }
}
