package com.paypal.android.corepayments.analytics

data class AnalyticsEventData(
    val clientID: String,
    val environment: String,
    val eventName: String,
    val timestamp: Long,
    val sessionID: String,
    val appId: String,
    val appName: String,
    val clientSDKVersion: String,
    val clientOS: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val isSimulator: Boolean,
    val merchantAppVersion: String
) {
    constructor(
        clientID: String,
        environment: String,
        eventName: String,
        timestamp: Long,
        sessionID: String,
        deviceData: DeviceData,
    ) : this(
        clientID,
        environment,
        eventName,
        timestamp,
        sessionID,
        deviceData.appId,
        deviceData.appName,
        deviceData.clientSDKVersion,
        deviceData.clientOS,
        deviceData.deviceManufacturer,
        deviceData.deviceModel,
        deviceData.isSimulator,
        deviceData.merchantAppVersion
    )
}
