package com.paypal.android.core.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.paypal.android.core.BuildConfig

data class DeviceInspector internal constructor(
    val appId: String,
    val appName: String,
    val merchantAppVersion: String,
    val clientSDKVersion: String,
    private val sdkInt: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val deviceProduct: String,
    val deviceFingerprint: String
) {

    companion object {

        fun parseAppName(context: Context): String = try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (ignored: PackageManager.NameNotFoundException) {
            "N/A"
        }

        fun parseAppVersion(context: Context): String = try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (ignored: PackageManager.NameNotFoundException) {
            "N/A"
        }
    }

    constructor(
        context: Context,
        clientSDKVersion: String = BuildConfig.CLIENT_SDK_VERSION,
        sdkInt: Int = Build.VERSION.SDK_INT,
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL,
        deviceProduct: String = Build.PRODUCT,
        deviceFingerprint: String = Build.FINGERPRINT
    ) : this(
        appId = context.packageName,
        appName = parseAppName(context),
        merchantAppVersion = parseAppVersion(context),
        clientSDKVersion = clientSDKVersion,
        sdkInt = sdkInt,
        deviceManufacturer = deviceManufacturer,
        deviceModel = deviceModel,
        deviceProduct = deviceProduct,
        deviceFingerprint = deviceFingerprint
    )

    val clientOS = "Android API $sdkInt"

    val isSimulator = "google_sdk".equals(deviceProduct, ignoreCase = true) ||
            "sdk".equals(deviceProduct, ignoreCase = true) ||
            "Genymotion".equals(deviceManufacturer, ignoreCase = true) ||
            deviceFingerprint.contains("generic")
}
