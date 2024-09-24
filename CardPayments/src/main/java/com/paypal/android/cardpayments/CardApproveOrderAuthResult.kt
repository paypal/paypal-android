package com.paypal.android.cardpayments

import android.net.Uri
import com.paypal.android.corepayments.PayPalSDKError

sealed class CardApproveOrderAuthResult {

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
    ) :
        CardApproveOrderAuthResult()

    data class Failure(val error: PayPalSDKError, val orderId: String? = null) : CardApproveOrderAuthResult()
    object NoResult : CardApproveOrderAuthResult()
}