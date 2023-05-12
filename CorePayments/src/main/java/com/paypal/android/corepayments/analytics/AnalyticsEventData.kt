package com.paypal.android.corepayments.analytics

import org.json.JSONObject

data class AnalyticsEventData(
    val clientID: String,
    val environment: String,
    val eventName: String,
    val timestamp: Long,
    val orderId: String?,
    val deviceData: DeviceData
) {

    companion object {
        const val KEY_APP_ID = "app_id"
        const val KEY_APP_NAME = "app_name"
        const val KEY_CLIENT_ID = "partner_client_id"
        const val KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        const val KEY_CLIENT_OS = "client_os"
        const val KEY_COMPONENT = "comp"
        const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        const val KEY_DEVICE_MODEL = "mobile_device_model"
        const val KEY_ENVIRONMENT = "merchant_sdk_env"
        const val KEY_EVENT_NAME = "event_name"
        const val KEY_EVENT_SOURCE = "event_source"
        const val KEY_IS_SIMULATOR = "is_simulator"
        const val KEY_MERCHANT_APP_VERSION = "mapv"
        const val KEY_ORDER_ID = "order_id"
        const val KEY_PLATFORM = "platform"
        const val KEY_TIMESTAMP = "t"
        const val KEY_TENANT_NAME = "tenant_name"

        const val KEY_EVENT_PARAMETERS = "event_params"
        const val KEY_EVENTS = "events"
    }

    fun toJSON(): JSONObject {
        val eventParams = JSONObject()
            .put(KEY_APP_ID, deviceData.appId)
            .put(KEY_APP_NAME, deviceData.appName)
            .put(KEY_CLIENT_ID, clientID)
            .put(KEY_CLIENT_SDK_VERSION, deviceData.clientSDKVersion)
            .put(KEY_CLIENT_OS, deviceData.clientOS)
            .put(KEY_COMPONENT, "ppcpmobilesdk")
            .put(KEY_DEVICE_MANUFACTURER, deviceData.deviceManufacturer)
            .put(KEY_DEVICE_MODEL, deviceData.deviceModel)
            .put(KEY_ENVIRONMENT, environment)
            .put(KEY_EVENT_NAME, eventName)
            .put(KEY_EVENT_SOURCE, "mobile-native")
            .put(KEY_IS_SIMULATOR, deviceData.isSimulator)
            .put(KEY_MERCHANT_APP_VERSION, deviceData.merchantAppVersion)
            .put(KEY_ORDER_ID, orderId)
            .put(KEY_PLATFORM, "Android")
            .put(KEY_TIMESTAMP, timestamp.toString())
            .put(KEY_TENANT_NAME, "PayPal")

        val midLevel = JSONObject()
            .put(KEY_EVENT_PARAMETERS, eventParams)

        return JSONObject()
            .put(KEY_EVENTS, midLevel)
    }
}
