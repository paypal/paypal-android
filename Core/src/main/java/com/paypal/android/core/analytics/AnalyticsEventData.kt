package com.paypal.android.core.analytics

import org.json.JSONObject

data class AnalyticsEventData(
    val eventName: String,
    val timestamp: Long,
    val sessionID: String,
    val deviceInspector: DeviceInspector,
) {

    companion object {
        const val KEY_APP_ID = "app_id"
        const val KEY_APP_NAME = "app_name"
        const val KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        const val KEY_CLIENT_OS = "client_os"
        const val KEY_COMPONENT = "comp"
        const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        const val KEY_DEVICE_MODEL = "mobile_device_model"
        const val KEY_EVENT_NAME = "event_name"
        const val KEY_EVENT_SOURCE = "event_source"
        const val KEY_IS_SIMULATOR = "is_simulator"
        const val KEY_MERCHANT_APP_VERSION = "mapv"
        const val KEY_PLATFORM = "platform"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_TIMESTAMP = "t"
        const val KEY_TENANT_NAME = "tenant_name"

        const val KEY_EVENT_PARAMETERS = "event_params"
        const val KEY_EVENTS = "events"
    }

    fun toJSON(): JSONObject {
        val eventParams = JSONObject()
            .put(KEY_APP_ID, deviceInspector.appId)
            .put(KEY_APP_NAME, deviceInspector.appName)
            .put(KEY_CLIENT_SDK_VERSION, deviceInspector.clientSDKVersion)
            .put(KEY_CLIENT_OS, deviceInspector.clientOS)
            .put(KEY_COMPONENT, "ppunifiedsdk")
            .put(KEY_DEVICE_MANUFACTURER, deviceInspector.deviceManufacturer)
            .put(KEY_DEVICE_MODEL, deviceInspector.deviceModel)
            .put(KEY_EVENT_NAME, eventName)
            .put(KEY_EVENT_SOURCE, "mobile-native")
            .put(KEY_IS_SIMULATOR, deviceInspector.isSimulator)
            .put(KEY_MERCHANT_APP_VERSION, deviceInspector.merchantAppVersion)
            .put(KEY_PLATFORM, "Android")
            .put(KEY_SESSION_ID, sessionID)
            .put(KEY_TIMESTAMP, timestamp.toString())
            .put(KEY_TENANT_NAME, "PayPal")

        val midLevel = JSONObject()
            .put(KEY_EVENT_PARAMETERS, eventParams)

        return JSONObject()
            .put(KEY_EVENTS, midLevel)
    }
}
