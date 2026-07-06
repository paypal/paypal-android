package com.paypal.android.customenvironment

import androidx.compose.runtime.Composable

/**
 * No-op stub for release builds. The settings screen is debug-only and unreachable
 * in release (the gear icon is hidden via BuildConfig.DEBUG).
 */
@Composable
fun SettingsView() {}
