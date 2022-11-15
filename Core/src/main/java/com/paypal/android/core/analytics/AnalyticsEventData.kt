package com.paypal.android.core.analytics

import android.os.Build
import com.paypal.android.core.BuildConfig
import com.paypal.android.core.analytics.models.DeviceData
import org.json.JSONObject
import java.sql.Timestamp
import java.util.*

data class AnalyticsEventData(
    val eventName: String,
    val sessionID: String,
    val deviceData: DeviceData,
    val timestamp: Long,
) {

    // TODO: - Add VERSION_NAME to BuildConfig
    private val clientSDKVersion = BuildConfig.PAYPAL_SDK_VERSION

    private val clientOS = "Android API ${Build.VERSION.SDK_INT}"

    private val component = "ppunifiedsdk"

    private val deviceManufacturer = Build.MANUFACTURER

    private val deviceModel = Build.MODEL

    private val eventSource = "mobile-native"

    private val platform = "Android"

    private val tenantName = "PayPal"

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
            .put(KEY_APP_ID, deviceData.appId)
            .put(KEY_APP_NAME, deviceData.appName)
            .put(KEY_CLIENT_SDK_VERSION, clientSDKVersion)
            .put(KEY_CLIENT_OS, clientOS)
            .put(KEY_COMPONENT, component)
            .put(KEY_DEVICE_MANUFACTURER, deviceManufacturer.orEmpty())
            .put(KEY_DEVICE_MODEL, deviceModel.orEmpty())
            .put(KEY_EVENT_NAME, eventName)
            .put(KEY_EVENT_SOURCE, eventSource)
            .put(KEY_IS_SIMULATOR, deviceData.isSimulator)
            .put(KEY_MERCHANT_APP_VERSION, deviceData.merchantAppVersion)
            .put(KEY_PLATFORM, platform)
            .put(KEY_SESSION_ID, sessionID)
            .put(KEY_TIMESTAMP, timestamp.toString())
            .put(KEY_TENANT_NAME, tenantName)

        val midLevel = JSONObject()
            .put(KEY_EVENT_PARAMETERS, eventParams)

        return JSONObject()
            .put(KEY_EVENTS, midLevel)
    }
}
