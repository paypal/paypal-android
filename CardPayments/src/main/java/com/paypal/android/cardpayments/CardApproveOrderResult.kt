package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardApproveOrderResult {

    data class Success(
        val orderId: String,
        val status: String? = null,
        val didAttemptThreeDSecureAuthentication: Boolean = false
    ) : CardApproveOrderResult()

    data class AuthorizationRequired(val authChallenge: CardAuthChallenge) : CardApproveOrderResult()
    data class Failure(val error: PayPalSDKError) : CardApproveOrderResult()
}
