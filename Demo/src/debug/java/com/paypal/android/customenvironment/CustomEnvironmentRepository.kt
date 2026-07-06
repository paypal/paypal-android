package com.paypal.android.customenvironment

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.paypal.android.corepayments.CoreConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists and retrieves [CustomEnvironmentConfig] via SharedPreferences.
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

    /** Returns the currently saved config (all fields blank if nothing has been saved). */
    fun getConfig(): CustomEnvironmentConfig = CustomEnvironmentConfig(
        sdkRestUrl = (prefs.getString(KEY_SDK_REST_URL, "") ?: "").trim().trimEnd('/'),
        sdkGraphQLUrl = (prefs.getString(KEY_SDK_GRAPHQL_URL, "") ?: "").trim().trimEnd('/'),
        clientId = (prefs.getString(KEY_CLIENT_ID, "") ?: "").trim(),
    )

    /** Persists [config] to SharedPreferences. */
    fun saveConfig(config: CustomEnvironmentConfig) {
        prefs.edit {
            putString(KEY_SDK_REST_URL, config.sdkRestUrl)
                .putString(KEY_SDK_GRAPHQL_URL, config.sdkGraphQLUrl)
            .putString(KEY_CLIENT_ID, config.clientId)
        }
    }

    /** Clears all saved values, reverting to the default sandbox environment. */
    fun clearConfig() {
        prefs.edit { clear() }
    }

    /**
     * Returns a [CoreConfig] for the active environment.
     * Uses [Environment.CUSTOM] when the user has configured custom URLs in Settings;
     * falls back to [fallbackConfig] otherwise.
     */
    fun getCoreConfig(fallbackConfig: CoreConfig): CoreConfig =
        getConfig().toCoreConfig(fallbackConfig)

    companion object {
        private const val PREFS_NAME = "custom_environment"
        private const val KEY_SDK_REST_URL = "sdk_rest_url"
        private const val KEY_SDK_GRAPHQL_URL = "sdk_graphql_url"
        private const val KEY_CLIENT_ID = "client_id"
    }
}
