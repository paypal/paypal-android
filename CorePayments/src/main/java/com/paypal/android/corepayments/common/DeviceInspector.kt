package com.paypal.android.corepayments.common

import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.core.net.toUri

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeviceInspector(private val context: Context) {

    val isPayPalInstalled: Boolean
        get() = isAppInstalled(PAYPAL_APP_PACKAGE)

    private fun isAppInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    fun isDeepLinkConfiguredInManifest(returnUrlScheme: String): Boolean {
        val testUri = "${returnUrlScheme}://".toUri()
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, testUri)
        deepLinkIntent.addCategory(Intent.CATEGORY_DEFAULT)
        deepLinkIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        val candidateActivities =
            context.packageManager.queryIntentActivities(deepLinkIntent, 0)
        return candidateActivities.isNotEmpty()
    }

    companion object {
        const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
    }
}
