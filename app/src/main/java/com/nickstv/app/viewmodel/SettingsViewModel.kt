package com.nickstv.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickstv.app.util.OtaUpdateManager
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val otaManager = OtaUpdateManager()
    val updateState = otaManager.updateState

    fun checkForUpdates() = viewModelScope.launch { otaManager.checkForUpdates() }

    fun downloadUpdate(release: com.nickstv.app.model.GitHubRelease) {
        otaManager.downloadUpdate(release)
    }

    fun resetUpdateState() = otaManager.reset()
}
