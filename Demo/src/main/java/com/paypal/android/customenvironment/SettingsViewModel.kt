package com.paypal.android.customenvironment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val customEnvironmentRepository: CustomEnvironmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(settings = customEnvironmentRepository.getConfig())
    )
    val uiState = _uiState.asStateFlow()

    private var saveSuccessJob: Job? = null

    fun updateSelectedEnvironment(value: SelectedEnvironment) {
        val savedSettings = customEnvironmentRepository.getConfig()
        _uiState.value = SettingsUiState(settings = savedSettings.copy(selectedEnvironment = value))
    }

    fun updateCustomSdkRestUrl(value: String) {
        _uiState.update { it.copy(settings = it.settings.copy(customSdkRestUrl = value), restUrlError = null, showSaveSuccess = false) }
    }

    fun updateCustomSdkGraphQLUrl(value: String) {
        _uiState.update { it.copy(settings = it.settings.copy(customSdkGraphQLUrl = value), graphQLUrlError = null, showSaveSuccess = false) }
    }

    fun updateCustomClientId(value: String) {
        _uiState.update { it.copy(settings = it.settings.copy(customClientId = value), showSaveSuccess = false) }
    }

    /** Validates URLs (CUSTOM only) then persists to SharedPreferences. */
    fun saveConfig() {
        val settings = _uiState.value.settings
        if (settings.selectedEnvironment != SelectedEnvironment.CUSTOM) {
            customEnvironmentRepository.saveConfig(settings)
            return
        }
        val restError = validateUrl(settings.customSdkRestUrl)
        val graphQLError = validateUrl(settings.customSdkGraphQLUrl)
        if (restError != null || graphQLError != null) {
            _uiState.update { it.copy(restUrlError = restError, graphQLUrlError = graphQLError) }
            // Persist whichever fields are valid so they survive an environment switch.
            // Invalid fields are saved as empty to avoid persisting bad data.
            customEnvironmentRepository.saveConfig(
                settings.copy(
                    customSdkRestUrl = if (restError == null) settings.customSdkRestUrl else "",
                    customSdkGraphQLUrl = if (graphQLError == null) settings.customSdkGraphQLUrl else "",
                )
            )
            return
        }
        customEnvironmentRepository.saveConfig(settings)
        showSaveSuccessBriefly()
    }

    /** Clears the saved config and resets all fields to their defaults. */
    fun clearConfig() {
        customEnvironmentRepository.clearConfig()
        _uiState.value = SettingsUiState()
    }

    private fun showSaveSuccessBriefly() {
        saveSuccessJob?.cancel()
        _uiState.update { it.copy(showSaveSuccess = true) }
        saveSuccessJob = viewModelScope.launch {
            delay(2_000)
            _uiState.update { it.copy(showSaveSuccess = false) }
        }
    }

    private fun validateUrl(url: String): String? {
        if (url.isBlank()) return "URL is required"
        if (url != url.trim()) return "URL must not contain leading or trailing spaces"
        if (url.contains(' ')) return "URL must not contain spaces"
        return try {
            val parsed = URL(url)
            when {
                parsed.protocol != "https" -> "URL must start with https://"
                parsed.host.isNullOrBlank() -> "Enter a valid URL (e.g. https://api.example.com)"
                else -> null
            }
        } catch (e: MalformedURLException) {
            "Enter a valid URL (e.g. https://api.example.com)"
        }
    }
}
