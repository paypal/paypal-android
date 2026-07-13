package com.paypal.android.customenvironment

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.paypal.android.api.services.MerchantIntegration
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists and retrieves [DemoEnvironmentSettings] via SharedPreferences.
 *
 * Injected as a singleton so all ViewModels share the same source of truth.
 */
@Singleton
class CustomEnvironmentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns the currently saved settings (defaults to SANDBOX with all fields blank if nothing saved). */
    fun getConfig(): DemoEnvironmentSettings = DemoEnvironmentSettings(
        selectedEnvironment = getSelectedEnvironment(),
        customSdkRestUrl = (prefs.getString(KEY_SDK_REST_URL, "") ?: ""),
        customSdkGraphQLUrl = (prefs.getString(KEY_SDK_GRAPHQL_URL, "") ?: ""),
        customClientId = (prefs.getString(KEY_CLIENT_ID, "") ?: ""),
        customMerchantBaseUrl = (prefs.getString(KEY_MERCHANT_BASE_URL, "") ?: ""),
        customVenmoCheckoutUrlPrefix = (prefs.getString(KEY_VENMO_CHECKOUT_URL_PREFIX, "") ?: ""),
        customMerchantId = (prefs.getString(KEY_MERCHANT_ID, "") ?: ""),
    )

    /** Persists [settings] to SharedPreferences. */
    fun saveConfig(settings: DemoEnvironmentSettings) {
        prefs.edit {
            putString(KEY_SELECTED_ENV, settings.selectedEnvironment.name)
            putString(KEY_SDK_REST_URL, settings.customSdkRestUrl)
            putString(KEY_SDK_GRAPHQL_URL, settings.customSdkGraphQLUrl)
            putString(KEY_CLIENT_ID, settings.customClientId)
            putString(KEY_MERCHANT_BASE_URL, settings.customMerchantBaseUrl)
            putString(KEY_VENMO_CHECKOUT_URL_PREFIX, settings.customVenmoCheckoutUrlPrefix)
            putString(KEY_MERCHANT_ID, settings.customMerchantId)
        }
    }

    /** Clears all saved values, reverting to the default sandbox environment. */
    fun clearConfig() {
        prefs.edit { clear() }
    }

    /**
     * Returns the merchant server base URL to use. When [SelectedEnvironment.CUSTOM] is active
     * and a custom merchant URL has been saved, that URL is returned. Otherwise falls back to the
     * default merchant server URL.
     */
    fun getMerchantBaseUrl(): String {
        val settings = getConfig()
        val customUrl = settings.customMerchantBaseUrl.trim()
        return if (settings.selectedEnvironment == SelectedEnvironment.CUSTOM && customUrl.isNotBlank()) {
            normalizeBaseUrl(customUrl)
        } else {
            MerchantIntegration.DEFAULT.baseUrl
        }
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trimEnd('/')
        return "$trimmed/"
    }

    /**
     * Returns a [CoreConfig] for the active environment.
     * - [SelectedEnvironment.LIVE] / [SelectedEnvironment.SANDBOX] → the corresponding [Environment].
     * - [SelectedEnvironment.CUSTOM] with URLs configured → [Environment.CUSTOM] with those URLs.
     * - [SelectedEnvironment.CUSTOM] without URLs → falls back to [fallbackConfig].
     */
    fun getCoreConfig(fallbackConfig: CoreConfig): CoreConfig {
        val settings = getConfig()
        return when (settings.selectedEnvironment) {
            SelectedEnvironment.LIVE ->
                CoreConfig(
                    clientId = fallbackConfig.clientId,
                    environment = Environment.LIVE,
                    merchantId = fallbackConfig.merchantId
                )

            SelectedEnvironment.SANDBOX ->
                CoreConfig(
                    clientId = fallbackConfig.clientId,
                    environment = Environment.SANDBOX,
                    merchantId = fallbackConfig.merchantId
                )

            SelectedEnvironment.CUSTOM -> if (settings.isValidEnvironment) {
                Environment.customRestUrl = settings.customSdkRestUrl.trim().trimEnd('/')
                Environment.customGraphQLUrl = settings.customSdkGraphQLUrl.trim().trimEnd('/')
                if (settings.customVenmoCheckoutUrlPrefix.isNotBlank()) {
                    val prefix = settings.customVenmoCheckoutUrlPrefix.trim()
                    Environment.customVenmoCheckoutBaseUrl =
                        "https://account.$prefix.venmo.com/go/web/paypal"
                }
                val resolvedClientId =
                    settings.customClientId.trim().ifBlank { fallbackConfig.clientId }
                val resolvedMerchantId =
                    settings.customMerchantId.trim().ifBlank { fallbackConfig.merchantId }
                CoreConfig(
                    clientId = resolvedClientId,
                    environment = Environment.CUSTOM,
                    merchantId = resolvedMerchantId
                )
            } else {
                fallbackConfig
            }
        }
    }

    private fun getSelectedEnvironment(): SelectedEnvironment {
        return prefs.getString(KEY_SELECTED_ENV, null)
            ?.let { runCatching { SelectedEnvironment.valueOf(it) }.getOrNull() }
            ?: SelectedEnvironment.SANDBOX
    }

    companion object {
        private const val PREFS_NAME = "custom_environment"
        private const val KEY_SELECTED_ENV = "selected_env"
        private const val KEY_SDK_REST_URL = "sdk_rest_url"
        private const val KEY_SDK_GRAPHQL_URL = "sdk_graphql_url"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_MERCHANT_BASE_URL = "merchant_base_url"
        private const val KEY_VENMO_CHECKOUT_URL_PREFIX = "venmo_checkout_url_prefix"
        private const val KEY_MERCHANT_ID = "merchant_id"
    }
}
