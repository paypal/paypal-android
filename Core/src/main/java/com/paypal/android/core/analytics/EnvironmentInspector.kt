package com.paypal.android.core.analytics

import android.content.Context
import com.paypal.android.core.analytics.models.DeviceData

class EnvironmentInspector(
    private val context: Context
) {
    val appName: String
        get() = try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (ex: Exception) {
            DEFAULT_APP_NAME
        }

    fun inspect(): DeviceData = DeviceData("app name", "app id", false, "merchant app version")

    companion object {
        const val DEFAULT_APP_NAME = "Default app name"
    }
}
