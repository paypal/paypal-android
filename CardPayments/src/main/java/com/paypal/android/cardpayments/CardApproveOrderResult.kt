package com.paypal.android.cardpayments

import android.net.Uri
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.BrowserSwitchAuthChallenge
import com.paypal.android.corepayments.PayPalSDKError

sealed class CardApproveOrderResult {

    data class Success(
        val orderId: String,
        /**
         * @suppress
         */
        @Deprecated("Use status instead.")
        val deepLinkUrl: Uri? = null,
        @Deprecated("Use didAttemptThreeDSecureAuthentication instead.")
        val liabilityShift: String? = null,
        val status: String? = null,
    ) : CardApproveOrderResult()

    data class AuthorizationRequired(val authChallenge: CardAuthChallenge) :
        CardApproveOrderResult()

    data class Failure(val error: PayPalSDKError) : CardApproveOrderResult()
}