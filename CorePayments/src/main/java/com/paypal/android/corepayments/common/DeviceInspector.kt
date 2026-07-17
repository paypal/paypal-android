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

    /**
     * The installed PayPal app's `versionName` (e.g. "9.1.0"), or `null` if the app isn't
     * installed or its version can't be read. Callers decide what to do with this (e.g. gating
     * app-switch eligibility to a minimum major version) — this just surfaces the raw value.
     */
    val payPalAppVersionName: String?
        get() = runCatching {
            context.packageManager.getPackageInfo(PAYPAL_APP_PACKAGE, 0).versionName
        }.getOrNull()

    private fun isAppInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    }.getOrDefault(false)

    /**
     * Whether [uri] actually resolves to the PayPal app, not just whether it's installed — the
     * user may have unchecked "Open supported links" for it. Mirrors Braintree Android's
     * `ResolvePayPalUseCase`.
     *
     * Also requires the installed PayPal app to meet [APP_SWITCH_MIN_MAJOR_VERSION], the minimum
     * major version that supports this SDK's app-switch flow. If the installed app's version
     * can't be determined, this fails closed (returns false) rather than risk switching into a
     * build that doesn't support it.
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
        return resolvesToPayPalApp && isAppSwitchSupportedVersion(payPalAppVersionName)
    }

    /**
     * Parses the leading major-version integer from [versionName] (e.g. "9.1.0" -> 9) and
     * checks whether it meets [APP_SWITCH_MIN_MAJOR_VERSION]. Returns false if [versionName] is
     * null or its leading segment isn't a parseable integer.
     */
    private fun isAppSwitchSupportedVersion(versionName: String?): Boolean {
        val majorVersion = versionName?.substringBefore('.')?.toIntOrNull()
        return majorVersion != null && majorVersion >= APP_SWITCH_MIN_MAJOR_VERSION
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
        private const val APP_SWITCH_MIN_MAJOR_VERSION = 9
        private val DEFAULT_APP_SWITCH_URI: Uri
            get() = PAYPAL_APP_SWITCH_URL.toUri()
    }
}
