package com.paypal.android.corepayments.common

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeviceInspector(private val context: Context) {

    val isPayPalInstalled: Boolean
        get() = isAppInstalled(PAYPAL_APP_PACKAGE)

    private fun isAppInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    fun isDeepLinkConfiguredInManifest(context: Context, returnUrlScheme: String): Boolean {
        // TODO: implement
        return false
    }

    companion object {
        const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
    }
}
