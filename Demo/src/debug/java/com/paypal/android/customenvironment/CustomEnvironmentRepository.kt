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
 */
@Singleton
class CustomEnvironmentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns the currently saved config (both fields blank if nothing has been saved). */
    fun getConfig(): CustomEnvironmentConfig = CustomEnvironmentConfig(
        sdkRestUrl = (prefs.getString(KEY_SDK_REST_URL, "") ?: "").trim().trimEnd('/'),
        sdkGraphQLUrl = (prefs.getString(KEY_SDK_GRAPHQL_URL, "") ?: "").trim().trimEnd('/'),
    )

    /** Persists [config] to SharedPreferences. */
    fun saveConfig(config: CustomEnvironmentConfig) {
        prefs.edit()
            .putString(KEY_SDK_REST_URL, config.sdkRestUrl)
            .putString(KEY_SDK_GRAPHQL_URL, config.sdkGraphQLUrl)
            .apply()
    }

    /** Clears all saved values, reverting to the default sandbox environment. */
    fun clearConfig() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_environment"
        private const val KEY_SDK_REST_URL = "sdk_rest_url"
        private const val KEY_SDK_GRAPHQL_URL = "sdk_graphql_url"
    }
}
