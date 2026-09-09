package com.paypal.android.corepayments.browserswitch

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.auth.AuthTabIntent

internal class AuthenticateUserResultContract : ActivityResultContract<Intent, AuthTabResult>() {
    override fun createIntent(context: Context, input: Intent) = input

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
