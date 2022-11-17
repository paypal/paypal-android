package com.paypal.android.core.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.paypal.android.core.BuildConfig
import com.paypal.android.core.analytics.models.DeviceData

class DeviceInspector internal constructor(
    private val clientSDKVersion: String,
    private val sdkInt: Int,
    private val deviceManufacturer: String,
    private val deviceModel: String,
    private val deviceProduct: String,
    private val deviceFingerprint: String
) {

    constructor() : this(
        clientSDKVersion = BuildConfig.PAYPAL_SDK_VERSION,
        sdkInt = Build.VERSION.SDK_INT,
        deviceManufacturer = Build.MANUFACTURER,
        deviceModel = Build.MODEL,
        deviceProduct = Build.PRODUCT,
        deviceFingerprint = Build.FINGERPRINT
    )

    fun inspect(context: Context): DeviceData {
        val packageManager = context.packageManager

        val appName = try {
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (ex: PackageManager.NameNotFoundException) {
            ""
        }

        val merchantAppVersion = try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (ignored: PackageManager.NameNotFoundException) {
            ""
        }

        val appId = context.packageName
        val clientOS = "Android API $sdkInt"

        val isSimulator = "google_sdk".equals(deviceProduct, ignoreCase = true) ||
                "sdk".equals(deviceProduct, ignoreCase = true) ||
                "Genymotion".equals(deviceManufacturer, ignoreCase = true) ||
                deviceFingerprint.contains("generic")

        return DeviceData(
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
}
