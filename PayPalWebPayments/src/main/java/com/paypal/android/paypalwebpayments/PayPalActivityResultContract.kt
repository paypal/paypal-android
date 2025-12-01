package com.paypal.android.paypalwebpayments

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.auth.AuthTabIntent
import androidx.browser.auth.AuthTabIntent.EXTRA_REDIRECT_SCHEME
import androidx.browser.auth.AuthTabIntent.RESULT_UNKNOWN_CODE
import androidx.browser.auth.AuthTabIntent.RESULT_VERIFICATION_FAILED
import androidx.browser.auth.AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT

class PayPalAuthResult(val resultCode: Int, val resultUri: Uri?)

class PayPalActivityResultContract : ActivityResultContract<Uri, PayPalAuthResult>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        val intent = AuthTabIntent.Builder().build().intent
        intent.data = input
        intent.putExtra(EXTRA_REDIRECT_SCHEME, "com.paypal.android.demo")
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PayPalAuthResult {
        var resultCode = resultCode
        var resultUri: Uri? = null
        when (resultCode) {
            RESULT_OK -> resultUri = intent?.data
            RESULT_CANCELED, RESULT_VERIFICATION_FAILED, RESULT_VERIFICATION_TIMED_OUT -> {}
            else -> resultCode = RESULT_UNKNOWN_CODE
        }
        return PayPalAuthResult(resultCode, resultUri)
    }
}
