package com.paypal.android.customenvironment

import com.paypal.android.corepayments.CoreConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op stub for release builds. Custom environment configuration is a debug-only feature;
 * this stub always falls back to the default [CoreConfig].
 */
@Singleton
class CustomEnvironmentRepository @Inject constructor() {
    fun getCoreConfig(fallbackConfig: CoreConfig): CoreConfig = fallbackConfig
}
