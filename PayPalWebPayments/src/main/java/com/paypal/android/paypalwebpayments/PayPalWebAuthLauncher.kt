package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError

class PayPalWebAuthLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {
    constructor() : this(BrowserSwitchClient())

    fun parseResult(context: Context, intent: Intent?): PayPalWebAuthChallengeResult? =
        browserSwitchClient.parseResult(context, 123, intent)?.let { browserSwitchResult ->
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> {
                    if (browserSwitchResult.deepLinkUrl != null && browserSwitchResult.requestMetadata != null) {
                        val webResult = PayPalDeepLinkUrlResult(
                            browserSwitchResult.deepLinkUrl!!,
                            browserSwitchResult.requestMetadata!!
                        )
                        if (!webResult.orderId.isNullOrBlank() && !webResult.payerId.isNullOrBlank()) {
                            return PayPalWebAuthChallengeSuccess(
                                webResult.orderId,
                                webResult.payerId
                            )
                        } else {
                            return PayPalWebAuthChallengeError(PayPalWebCheckoutError.malformedResultError)
                        }
                    } else {
                        return PayPalWebAuthChallengeError(PayPalWebCheckoutError.unknownError)
                    }
                }

                BrowserSwitchStatus.CANCELED -> {
                    return PayPalWebAuthChallengeError(PayPalWebCheckoutError.userCanceledError)
                }

                else -> PayPalWebAuthChallengeError(PayPalWebCheckoutError.userCanceledError)
            }
        }

    fun launch(activity: FragmentActivity, authChallenge: PayPalWebAuthChallenge) {
        browserSwitchClient.start(activity, authChallenge.browserSwitchOptions)
    }

    fun clearPendingRequests(context: Context) {
        browserSwitchClient.clearActiveRequests(context)
    }
}