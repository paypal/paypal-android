package com.paypal.android.customenvironment

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val customEnvironmentRepository: CustomEnvironmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(customEnvironmentRepository.getConfig())
    val uiState = _uiState.asStateFlow()

    fun updateSelectedEnvironment(value: SelectedEnvironment) {
        _uiState.update { it.copy(selectedEnvironment = value) }
    }

    fun updateCustomSdkRestUrl(value: String) {
        _uiState.update { it.copy(customSdkRestUrl = value) }
    }

    fun updateCustomSdkGraphQLUrl(value: String) {
        _uiState.update { it.copy(customSdkGraphQLUrl = value) }
    }

    fun updateCustomClientId(value: String) {
        _uiState.update { it.copy(customClientId = value) }
    }

    /** Persists the current UI state to SharedPreferences. */
    fun saveConfig() {
        customEnvironmentRepository.saveConfig(_uiState.value)
    }

    /** Clears the saved config and resets all fields to their defaults. */
    fun clearConfig() {
        customEnvironmentRepository.clearConfig()
        _uiState.value = DemoEnvironmentSettings()
    }
}
