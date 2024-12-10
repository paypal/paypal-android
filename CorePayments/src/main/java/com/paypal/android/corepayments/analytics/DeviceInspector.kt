package com.paypal.android.corepayments.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.paypal.android.corepayments.BuildConfig

@Suppress("LongParameterList")
internal class DeviceInspector @VisibleForTesting constructor(
    private val clientSDKVersion: String,
    private val sdkInt: Int,
    private val deviceManufacturer: String,
    private val deviceModel: String,
    private val deviceProduct: String,
    private val deviceFingerprint: String,
    private val context: Context
) {

    @Suppress("LongParameterList")
    constructor(context: Context) : this(
        clientSDKVersion = BuildConfig.CLIENT_SDK_VERSION,
        sdkInt = Build.VERSION.SDK_INT,
        deviceManufacturer = Build.MANUFACTURER,
        deviceModel = Build.MODEL,
        deviceProduct = Build.PRODUCT,
        deviceFingerprint = Build.FINGERPRINT,
        context = context.applicationContext
    )

    fun inspect(): DeviceData {
        val packageManager = context.packageManager

        val appName = try {
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (ignored: PackageManager.NameNotFoundException) {
            "N/A"
        }

        val merchantAppVersion = try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (ignored: PackageManager.NameNotFoundException) {
            "N/A"
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
            merchantAppVersion = merchantAppVersion ?: "",
        )
    }
}
