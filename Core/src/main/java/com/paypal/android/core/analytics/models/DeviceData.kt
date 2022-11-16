package com.paypal.android.core.analytics.models

data class DeviceData(
    val appName: String,
    val appId: String,
    val clientSDKVersion: String,
    val clientOS: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val isSimulator: Boolean,
    val merchantAppVersion: String,
)
