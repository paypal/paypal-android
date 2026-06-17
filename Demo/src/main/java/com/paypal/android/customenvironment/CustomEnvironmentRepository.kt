package com.paypal.android.customenvironment

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists and retrieves [CustomEnvironmentConfig] via SharedPreferences.
 *
 * Injected as a singleton so all ViewModels share the same source of truth.
 * Note: no stage URLs are stored in source control — they are entered at runtime
 * by the demo app user and stored only on-device.
 */
@Singleton
class CustomEnvironmentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns the currently saved config (all fields blank if nothing has been saved). */
    fun getConfig(): CustomEnvironmentConfig = CustomEnvironmentConfig(
        clientId = (prefs.getString(KEY_CLIENT_ID, "") ?: "").trim(),
        sdkRestUrl = (prefs.getString(KEY_SDK_REST_URL, "") ?: "").trim().trimEnd('/'),
        sdkGraphQLUrl = (prefs.getString(KEY_SDK_GRAPHQL_URL, "") ?: "").trim().trimEnd('/'),
        merchantServerUrl = (prefs.getString(KEY_MERCHANT_SERVER_URL, "") ?: "").trim().trimEnd('/'),
        createOrderPath = (prefs.getString(KEY_CREATE_ORDER_PATH, "") ?: "").trim(),
        createSetupTokenPath = (prefs.getString(KEY_CREATE_SETUP_TOKEN_PATH, "") ?: "").trim(),
        createPaymentTokenPath = (prefs.getString(KEY_CREATE_PAYMENT_TOKEN_PATH, "") ?: "").trim()
    )

    /** Persists [config] to SharedPreferences. */
    fun saveConfig(config: CustomEnvironmentConfig) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, config.clientId)
            .putString(KEY_SDK_REST_URL, config.sdkRestUrl)
            .putString(KEY_SDK_GRAPHQL_URL, config.sdkGraphQLUrl)
            .putString(KEY_MERCHANT_SERVER_URL, config.merchantServerUrl)
            .putString(KEY_CREATE_ORDER_PATH, config.createOrderPath)
            .putString(KEY_CREATE_SETUP_TOKEN_PATH, config.createSetupTokenPath)
            .putString(KEY_CREATE_PAYMENT_TOKEN_PATH, config.createPaymentTokenPath)
            .apply()
    }

    /** Clears all saved values, reverting to the default sandbox environment. */
    fun clearConfig() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_environment"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_SDK_REST_URL = "sdk_rest_url"
        private const val KEY_SDK_GRAPHQL_URL = "sdk_graphql_url"
        private const val KEY_MERCHANT_SERVER_URL = "merchant_server_url"
        private const val KEY_CREATE_ORDER_PATH = "create_order_path"
        private const val KEY_CREATE_SETUP_TOKEN_PATH = "create_setup_token_path"
        private const val KEY_CREATE_PAYMENT_TOKEN_PATH = "create_payment_token_path"
    }
}
