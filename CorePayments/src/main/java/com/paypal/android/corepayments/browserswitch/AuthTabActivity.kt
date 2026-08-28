package com.paypal.android.corepayments.browserswitch

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri

/**
 * Internal lifecycle owner for Auth Tab's Activity Result launcher.
 *
 * Keeping registration inside this SDK-owned Activity lets existing merchant integrations keep
 * forwarding return intents to PayPalClient.finishStart()/finishVault().
 */
internal class AuthTabActivity : ComponentActivity() {

    private val authTabLauncher = AuthTabIntent.registerActivityResultLauncher(
        this,
        ::handleAuthResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            launchAuthTab()
        }
    }

    private fun launchAuthTab() {
        val targetUri = intent.getStringExtra(EXTRA_TARGET_URI)?.toUri()
        val redirectScheme = intent.getStringExtra(EXTRA_REDIRECT_SCHEME)
        val appLinkUri = intent.getStringExtra(EXTRA_APP_LINK_URL)?.toUri()
        if (targetUri == null || (redirectScheme == null && appLinkUri == null)) {
            handleLaunchFailure()
            return
        }

        try {
            val authTabIntent = AuthTabIntent.Builder().build()
            if (redirectScheme != null) {
                authTabIntent.launch(authTabLauncher, targetUri, redirectScheme)
            } else {
                val host = appLinkUri?.host
                if (host == null) {
                    handleLaunchFailure()
                } else {
                    authTabIntent.launch(authTabLauncher, targetUri, host, appLinkUri.path.orEmpty())
                }
            }
        } catch (_: RuntimeException) {
            handleLaunchFailure()
        }
    }

    private fun handleAuthResult(result: AuthTabIntent.AuthResult) {
        val resultCode = if (result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri == null) {
            AuthTabIntent.RESULT_UNKNOWN_CODE
        } else {
            result.resultCode
        }
        returnToSourceActivity(resultCode, result.resultUri ?: getFallbackResultUri())
    }

    private fun handleLaunchFailure() {
        returnToSourceActivity(AuthTabIntent.RESULT_UNKNOWN_CODE, getFallbackResultUri())
    }

    private fun getFallbackResultUri(): Uri? {
        val appLinkUrl = intent.getStringExtra(EXTRA_APP_LINK_URL)
        if (!appLinkUrl.isNullOrBlank()) {
            return appLinkUrl.toUri()
        }

        return intent.getStringExtra(EXTRA_REDIRECT_SCHEME)?.let { scheme ->
            "$scheme://x-callback-url/paypal-sdk/paypal-checkout/cancel".toUri()
        }
    }

    private fun returnToSourceActivity(resultCode: Int, resultUri: Uri?) {
        val sourceComponent = intent.getStringExtra(EXTRA_SOURCE_COMPONENT)
            ?.let(ComponentName::unflattenFromString)
        if (sourceComponent != null) {
            val returnIntent = Intent(Intent.ACTION_VIEW, resultUri).apply {
                component = sourceComponent
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_AUTH_TAB_RESULT_CODE, resultCode)
            }
            startActivity(returnIntent)
        }
        finish()
    }

    companion object {
        internal const val EXTRA_AUTH_TAB_RESULT_CODE =
            "com.paypal.android.corepayments.extra.AUTH_TAB_RESULT_CODE"

        private const val EXTRA_TARGET_URI =
            "com.paypal.android.corepayments.extra.AUTH_TAB_TARGET_URI"
        private const val EXTRA_REDIRECT_SCHEME =
            "com.paypal.android.corepayments.extra.AUTH_TAB_REDIRECT_SCHEME"
        private const val EXTRA_APP_LINK_URL =
            "com.paypal.android.corepayments.extra.AUTH_TAB_APP_LINK_URL"
        private const val EXTRA_SOURCE_COMPONENT =
            "com.paypal.android.corepayments.extra.AUTH_TAB_SOURCE_COMPONENT"

        fun createIntent(context: Context, options: BrowserSwitchOptions) =
            Intent(context, AuthTabActivity::class.java).apply {
                putExtra(EXTRA_TARGET_URI, options.targetUri.toString())
                putExtra(EXTRA_REDIRECT_SCHEME, options.returnUrlScheme)
                putExtra(EXTRA_APP_LINK_URL, options.appLinkUrl)
                putExtra(EXTRA_SOURCE_COMPONENT, (context as? Activity)?.componentName?.flattenToString())
            }
    }
}
