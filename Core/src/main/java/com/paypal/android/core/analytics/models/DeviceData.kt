package com.paypal.android.core.analytics.models

data class DeviceData(
    val appName: String,
    val appId: String,
    val isSimulator: Boolean,
    val merchantAppVersion: String,
)
