package com.paypal.android.customenvironment

data class SettingsUiState(
    val settings: DemoEnvironmentSettings = DemoEnvironmentSettings(),
    val restUrlError: String? = null,
    val graphQLUrlError: String? = null,
    val merchantBaseUrlError: String? = null,
    val venmoEnvironmentError: String? = null,
    val venmoCheckoutUrlPrefixError: String? = null,
    val showSaveSuccess: Boolean = false,
)
