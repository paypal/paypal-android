package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri

/** Creates an Auth Tab launch intent and converts the browser's Activity result. */
internal class LaunchAuthTab : ActivityResultContract<BrowserSwitchOptions, AuthTabResult>() {

    override fun createIntent(context: Context, input: BrowserSwitchOptions): Intent {
        val appLinkUri = input.appLinkUrl?.toUri()
        return AuthTabIntent.Builder().build().intent.apply {
            data = input.targetUri
            if (input.returnUrlScheme != null) {
                putExtra(AuthTabIntent.EXTRA_REDIRECT_SCHEME, input.returnUrlScheme)
            } else {
                putExtra(AuthTabIntent.EXTRA_HTTPS_REDIRECT_HOST, appLinkUri?.host.orEmpty())
                putExtra(AuthTabIntent.EXTRA_HTTPS_REDIRECT_PATH, appLinkUri?.path.orEmpty())
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AuthTabResult {
        val normalizedResultCode = when (resultCode) {
            AuthTabIntent.RESULT_OK,
            AuthTabIntent.RESULT_CANCELED,
            AuthTabIntent.RESULT_VERIFICATION_FAILED,
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> resultCode
            else -> AuthTabIntent.RESULT_UNKNOWN_CODE
        }
        val resultUri = if (normalizedResultCode == AuthTabIntent.RESULT_OK) intent?.data else null
        return AuthTabResult(normalizedResultCode, resultUri)
    }
}
