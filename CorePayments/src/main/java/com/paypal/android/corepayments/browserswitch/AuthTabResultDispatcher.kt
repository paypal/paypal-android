package com.paypal.android.corepayments.browserswitch

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.browser.auth.AuthTabIntent

/** Sends the Auth Tab result through the merchant's existing deep-link receiver. */
internal class AuthTabResultDispatcher(private val applicationContext: Context) {

    fun dispatch(pendingRequest: PendingRequest, result: AuthTabResult) {
        val resultCode = if (
            result.resultCode == AuthTabIntent.RESULT_OK && result.resultUri == null
        ) {
            AuthTabIntent.RESULT_UNKNOWN_CODE
        } else {
            result.resultCode
        }
        val resultUri = result.resultUri ?: pendingRequest.fallbackResultUri ?: return
        val returnIntent = Intent(Intent.ACTION_VIEW, resultUri).apply {
            // Keep manifest resolution so the merchant's registered receiver is selected, while
            // preventing another app from intercepting the result.
            setPackage(applicationContext.packageName)
            putExtra(AuthTabClient.EXTRA_AUTH_TAB_RESULT_CODE, resultCode)
            putExtra(
                AuthTabClient.EXTRA_BROWSER_SWITCH_STATE,
                pendingRequest.encodedBrowserSwitchState,
            )
        }
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or
            PendingIntent.FLAG_ONE_SHOT or
            PendingIntent.FLAG_IMMUTABLE
        PendingIntent.getActivity(applicationContext, 0, returnIntent, flags).send()
    }
}
