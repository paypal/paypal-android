package com.paypal.android.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject

data class AnalyticsEventData(
    val eventName: String,
    val context: Context,
    val sessionID: String
) {

    private val appID = context.packageName

    private val appName: String
        get() {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            return if (applicationInfo != null) {
                context.packageManager.getApplicationLabel(applicationInfo) as String
            } else {
                ""
            }
        }

    // TODO: - Add VERSION_NAME to BuildConfig
    private val clientSDKVersion = ""

    private val clientOS = "Android API " + Build.VERSION.SDK_INT.toString()

    private val component = "ppunifiedsdk"

    private val deviceManufacturer = Build.MANUFACTURER

    private val deviceModel = Build.MODEL

    private val eventSource = "mobile-native"

    private val isSimulator: Boolean
        get() {
            return "google_sdk".equals(Build.PRODUCT, ignoreCase = true) ||
                    "sdk".equals(Build.PRODUCT, ignoreCase = true) ||
                    "Genymotion".equals(Build.MANUFACTURER, ignoreCase = true) ||
                    Build.FINGERPRINT.contains("generic")
        }

    private val merchantAppVersion: String
        get() {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (ignored: PackageManager.NameNotFoundException) {
                ""
            }
        }

    private val platform = "Android"

    private val timestamp = System.currentTimeMillis()

    private val tenantName = "PayPal"

    companion object {
        const val KEY_APP_ID = "app_id"
        const val KEY_APP_NAME = "app_name"
        const val KEY_CLIENT_SDK_VERSION = "c_sdk_ver"
        const val KEY_CLIENT_OS = "client_os"
        const val KEY_COMPONENT = "comp"
        const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        const val KEY_DEVICE_MODEL = "device_model"
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
            .put(KEY_APP_ID, appID)
            .put(KEY_APP_NAME, appName)
            .put(KEY_CLIENT_SDK_VERSION, clientSDKVersion)
            .put(KEY_CLIENT_OS, clientOS)
            .put(KEY_COMPONENT, component)
            .put(KEY_DEVICE_MANUFACTURER, deviceManufacturer)
            .put(KEY_DEVICE_MODEL, deviceModel)
            .put(KEY_EVENT_NAME, eventName)
            .put(KEY_EVENT_SOURCE, eventSource)
            .put(KEY_IS_SIMULATOR, isSimulator)
            .put(KEY_MERCHANT_APP_VERSION, merchantAppVersion)
            .put(KEY_DEVICE_MODEL, deviceModel)
            .put(KEY_PLATFORM, platform)
            .put(KEY_SESSION_ID, sessionID)
            .put(KEY_TIMESTAMP, timestamp)
            .put(KEY_TENANT_NAME, tenantName)

        val midLevel = JSONObject()
            .put(KEY_EVENT_PARAMETERS, eventParams)

        return JSONObject()
            .put(KEY_EVENTS, midLevel)
    }
}
