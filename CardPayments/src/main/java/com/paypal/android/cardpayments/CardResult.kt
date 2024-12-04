package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

object CardResult {

    sealed class ApproveOrder {

        data class Success(
            val orderId: String,
            val status: String? = null,
            val didAttemptThreeDSecureAuthentication: Boolean = false
        ) : ApproveOrder()

        data class AuthorizationRequired(val authChallenge: CardAuthChallenge) : ApproveOrder()
        data class Failure(val error: PayPalSDKError) : ApproveOrder()
    }

    sealed class FinishApproveOrder {
        data class Success(
            val orderId: String,
            val status: String? = null,
            val didAttemptThreeDSecureAuthentication: Boolean = false
        ) : FinishApproveOrder()

        data class Failure(val error: PayPalSDKError) : FinishApproveOrder()
        data object Canceled : FinishApproveOrder()
        data object NoResult : FinishApproveOrder()
    }
}