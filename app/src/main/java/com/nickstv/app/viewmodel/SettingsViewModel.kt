package com.nickstv.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickstv.app.NicksTVApp
import com.nickstv.app.repository.RealDebridRepository
import com.nickstv.app.util.OtaUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val otaManager = OtaUpdateManager()
    val updateState = otaManager.updateState

    private val rdRepo: RealDebridRepository = NicksTVApp.instance.realDebridRepository

    // RD token UI state
    private val _rdTokenInput = MutableStateFlow(rdRepo.apiToken)
    val rdTokenInput: StateFlow<String> = _rdTokenInput.asStateFlow()

    private val _rdValidationState = MutableStateFlow<RdValidationState>(
        if (rdRepo.hasToken) RdValidationState.Saved else RdValidationState.Idle
    )
    val rdValidationState: StateFlow<RdValidationState> = _rdValidationState.asStateFlow()

    fun updateRdTokenInput(value: String) { _rdTokenInput.value = value }

    fun saveAndValidateRdToken() {
        val token = _rdTokenInput.value.trim()
        if (token.isBlank()) {
            rdRepo.apiToken = ""
            _rdValidationState.value = RdValidationState.Idle
            return
        }
        _rdValidationState.value = RdValidationState.Validating
        viewModelScope.launch {
            val (ok, msg) = rdRepo.validateToken(token)
            if (ok) {
                rdRepo.apiToken = token
                _rdValidationState.value = RdValidationState.Valid(msg)
            } else {
                _rdValidationState.value = RdValidationState.Error(msg)
            }
        }
    }

    fun clearRdToken() {
        rdRepo.apiToken = ""
        _rdTokenInput.value = ""
        _rdValidationState.value = RdValidationState.Idle
    }

    fun checkForUpdates() = viewModelScope.launch { otaManager.checkForUpdates() }

    fun downloadUpdate(release: com.nickstv.app.model.GitHubRelease) {
        otaManager.downloadUpdate(release)
    }

    fun resetUpdateState() = otaManager.reset()

    sealed class RdValidationState {
        object Idle : RdValidationState()
        object Validating : RdValidationState()
        object Saved : RdValidationState()
        data class Valid(val username: String) : RdValidationState()
        data class Error(val message: String) : RdValidationState()
    }
}
