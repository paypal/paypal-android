package com.paypal.android.customenvironment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val customEnvironmentRepository: CustomEnvironmentRepository
) : ViewModel() {

    companion object {
        private const val SUCCESS_MESSAGE_DISPLAY_TIME: Long = 2_000
    }

    private val _uiState = MutableStateFlow(
        SettingsUiState(settings = customEnvironmentRepository.getConfig())
    )
    val uiState = _uiState.asStateFlow()

    fun updateSelectedEnvironment(value: SelectedEnvironment) {
        val savedSettings = customEnvironmentRepository.getConfig()
        _uiState.value = SettingsUiState(settings = savedSettings.copy(selectedEnvironment = value))
    }

    fun updateCustomSdkRestUrl(value: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(customSdkRestUrl = value),
                restUrlError = null,
                showSaveSuccess = false
            )
        }
    }

    fun updateCustomSdkGraphQLUrl(value: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(customSdkGraphQLUrl = value),
                graphQLUrlError = null,
                showSaveSuccess = false
            )
        }
    }

    fun updateCustomClientId(value: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(customClientId = value),
                showSaveSuccess = false
            )
        }
    }

    fun updateCustomMerchantBaseUrl(value: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(customMerchantBaseUrl = value),
                merchantBaseUrlError = null,
                showSaveSuccess = false
            )
        }
    }

    fun updateCustomVenmoCheckoutUrlPrefix(value: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(customVenmoCheckoutUrlPrefix = value),
                venmoCheckoutUrlPrefixError = null,
                showSaveSuccess = false
            )
        }
    }

    fun updateCustomMerchantId(value: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(customMerchantId = value),
                merchantIdError = null,
                showSaveSuccess = false
            )
        }
    }

    /** Validates URLs (CUSTOM only) then persists to SharedPreferences. */
    @Suppress("CyclomaticComplexMethod")
    fun saveConfig() {
        val settings = _uiState.value.settings
        if (settings.selectedEnvironment != SelectedEnvironment.CUSTOM) {
            customEnvironmentRepository.saveConfig(settings)
            return
        }
        val restError = validateUrl(settings.customSdkRestUrl)
        val graphQLError = validateUrl(settings.customSdkGraphQLUrl)
        val merchantError = validateUrl(settings.customMerchantBaseUrl)
        val venmoCheckoutPrefixError = if (settings.customVenmoCheckoutUrlPrefix.isNotBlank()) {
            validateVenmoCheckoutPrefix(settings.customVenmoCheckoutUrlPrefix)
        } else {
            null
        }
        val merchantIdError = if (settings.customMerchantId.isNotBlank()) {
            validateMerchantId(settings.customMerchantId)
        } else {
            null
        }

        val hasErrors = restError != null || graphQLError != null || merchantError != null ||
                venmoCheckoutPrefixError != null || merchantIdError != null
        if (hasErrors) {
            _uiState.update {
                it.copy(
                    restUrlError = restError,
                    graphQLUrlError = graphQLError,
                    merchantBaseUrlError = merchantError,
                    venmoCheckoutUrlPrefixError = venmoCheckoutPrefixError,
                    merchantIdError = merchantIdError
                )
            }
            // Persist whichever fields are valid so they survive an environment switch.
            // Invalid fields are saved as empty to avoid persisting bad data.
            customEnvironmentRepository.saveConfig(
                settings.copy(
                    customSdkRestUrl = if (restError == null) settings.customSdkRestUrl else "",
                    customSdkGraphQLUrl = if (graphQLError == null) settings.customSdkGraphQLUrl else "",
                    customMerchantBaseUrl = if (merchantError == null) settings.customMerchantBaseUrl else "",
                    customVenmoCheckoutUrlPrefix = if (venmoCheckoutPrefixError == null) {
                        settings.customVenmoCheckoutUrlPrefix
                    } else {
                        ""
                    },
                    customMerchantId = if (merchantIdError == null) settings.customMerchantId else "",
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
        _uiState.update { it.copy(showSaveSuccess = true) }
        viewModelScope.launch {
            delay(SUCCESS_MESSAGE_DISPLAY_TIME)
            _uiState.update { it.copy(showSaveSuccess = false) }
        }
    }

    /**
     * Validates a URL field. Both http:// and https:// are accepted.
     *
     * When [optional] is true, a blank value is considered valid (field is not required).
     * When [optional] is false, the field is required.
     */
    private fun validateUrl(url: String, optional: Boolean = false): String? = when {
        url.isBlank() -> if (optional) null else "URL is required"
        url != url.trim() || url.contains(' ') -> "URL must not contain spaces"
        else -> try {
            val uri = URI(url)
            when {
                uri.scheme !in listOf("http", "https") -> "URL must start with http:// or https://"
                uri.host.isNullOrBlank() -> "Enter a valid URL (e.g. https://api.example.com)"
                else -> null
            }
        } catch (_: URISyntaxException) {
            "Enter a valid URL (e.g. https://api.example.com)"
        }
    }

    private fun validateVenmoCheckoutPrefix(value: String): String? = when {
        value != value.trim() || value.contains(' ') -> "Prefix must not contain spaces"
        !value.matches(Regex("[a-zA-Z0-9-]+")) -> "Prefix must contain only alphanumeric characters and hyphens"
        else -> null
    }

    private fun validateMerchantId(value: String): String? {
        @Suppress("MagicNumber")
        val minimumLength = 3
        return when {
            value != value.trim() || value.contains(' ') -> "Merchant ID must not contain spaces"
            value.length < minimumLength -> "Merchant ID must be at least $minimumLength characters"
            else -> null
        }
    }
}
