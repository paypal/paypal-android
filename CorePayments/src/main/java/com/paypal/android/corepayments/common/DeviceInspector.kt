package com.paypal.android.corepayments.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeviceInspector(private val context: Context) {

    val isPayPalInstalled: Boolean
        get() = isAppInstalled(PAYPAL_APP_PACKAGE)

    private fun isAppInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    }.getOrDefault(false)

    /**
     * Whether [uri] actually resolves to the PayPal app, not just whether it's installed — the
     * user may have unchecked "Open supported links" for it. Mirrors Braintree Android's
     * `ResolvePayPalUseCase`.
     */
    fun canResolvePayPalAppSwitch(uri: Uri = DEFAULT_APP_SWITCH_URI): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolvedActivity = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolvedActivity?.activityInfo?.packageName == PAYPAL_APP_PACKAGE
    }

    fun isDeepLinkConfiguredInManifest(returnUrlScheme: String): Boolean {
        val testUri = "$returnUrlScheme://".toUri()
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, testUri)
        deepLinkIntent.addCategory(Intent.CATEGORY_DEFAULT)
        deepLinkIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        val candidateActivities =
            context.packageManager.queryIntentActivities(deepLinkIntent, 0)
        return candidateActivities.isNotEmpty()
    }

    companion object {
        const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
        private const val PAYPAL_APP_SWITCH_URL = "https://www.paypal.com/app-switch-checkout"
        private val DEFAULT_APP_SWITCH_URI: Uri
            get() = PAYPAL_APP_SWITCH_URL.toUri()
    }
}
