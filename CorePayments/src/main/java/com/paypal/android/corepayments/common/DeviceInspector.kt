package com.paypal.android.corepayments.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.net.toUri

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DeviceInspector(private val context: Context) {

    val isPayPalInstalled: Boolean
        get() = isAppInstalled(PAYPAL_APP_PACKAGE)

    /**
     * The installed PayPal app's version code (build number), or `null` if the app isn't
     * installed or its version can't be read. Uses [android.content.pm.PackageInfo.longVersionCode]
     * on API 28+ and falls back to the deprecated `versionCode` int below that.
     */
    private val payPalAppVersionCode: Long?
        get() = runCatching {
            val packageInfo = context.packageManager.getPackageInfo(PAYPAL_APP_PACKAGE, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        }.getOrNull()

    private fun isAppInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    }.getOrDefault(false)

    /**
     * Whether [uri] actually resolves to the PayPal app, not just whether it's installed — the
     * user may have unchecked "Open supported links" for it. Mirrors Braintree Android's
     * `ResolvePayPalUseCase`.
     *
     * Also requires the installed PayPal app's version code to be newer than
     * [MIN_APP_SWITCH_COMPATIBLE_PAYPAL_VERSION_CODE] — PayPal app v10.6.0
     * (build 1160090131), the last Play Store build shipped *without* the required
     * app-switch changes this SDK relies on. If the installed app's version code can't be
     * determined, this fails closed (returns false) rather than risk switching into a build that
     * doesn't support it.
     */
    fun canResolvePayPalAppSwitch(uri: Uri = DEFAULT_APP_SWITCH_URI): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolvedActivity = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val resolvesToPayPalApp = resolvedActivity?.activityInfo?.packageName == PAYPAL_APP_PACKAGE
        return resolvesToPayPalApp &&
                payPalAppVersionCode?.let { it > MIN_APP_SWITCH_COMPATIBLE_PAYPAL_VERSION_CODE } ?: false
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
        private const val MIN_APP_SWITCH_COMPATIBLE_PAYPAL_VERSION_CODE = 1_160_090_131L
        private val DEFAULT_APP_SWITCH_URI: Uri
            get() = PAYPAL_APP_SWITCH_URL.toUri()
    }
}
