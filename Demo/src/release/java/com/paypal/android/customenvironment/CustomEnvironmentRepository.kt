package com.paypal.android.customenvironment

import com.paypal.android.corepayments.CoreConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom environment configuration is a debug-only feature; all methods are no-ops in release.
 */
@Singleton
class CustomEnvironmentRepository @Inject constructor() {
    fun getConfig(): CustomEnvironmentConfig = CustomEnvironmentConfig()
    fun saveConfig(config: CustomEnvironmentConfig) = Unit
    fun clearConfig() = Unit
    fun getCoreConfig(fallbackConfig: CoreConfig): CoreConfig = fallbackConfig
}
