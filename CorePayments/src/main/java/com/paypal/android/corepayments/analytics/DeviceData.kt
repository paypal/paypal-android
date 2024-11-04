package com.paypal.android.corepayments.analytics

internal data class DeviceData(
    val appId: String,
    val appName: String,
    val clientOS: String,
    val clientSDKVersion: String,
    val merchantAppVersion: String?,
    val deviceManufacturer: String,
    val deviceModel: String,
    val isSimulator: Boolean,
)
