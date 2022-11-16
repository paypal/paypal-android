package com.paypal.android.core.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.paypal.android.core.BuildConfig
import com.paypal.android.core.analytics.models.DeviceData

class DeviceInspector(
    private val context: Context
) {

    private val appName: String
        get() = try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (ex: Exception) {
            ""
        }

    private val appId = context.packageName

    private val clientSDKVersion = BuildConfig.PAYPAL_SDK_VERSION

    private val clientOS = "Android API ${Build.VERSION.SDK_INT}"

    private val deviceManufacturer = Build.MANUFACTURER

    private val deviceModel = Build.MODEL

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

    fun inspect(): DeviceData = DeviceData(
        appName = appName,
        appId = appId,
        clientSDKVersion = clientSDKVersion,
        clientOS = clientOS,
        deviceManufacturer = deviceManufacturer,
        deviceModel = deviceModel,
        isSimulator = isSimulator,
        merchantAppVersion = merchantAppVersion
    )

}
