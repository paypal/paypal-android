package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardFinishApproveOrderResult {
    data class Success(
        val orderId: String,
        val status: String? = null,
        val didAttemptThreeDSecureAuthentication: Boolean = false
    ) : CardFinishApproveOrderResult()

    data class Failure(val error: PayPalSDKError) : CardFinishApproveOrderResult()
    data object Canceled : CardFinishApproveOrderResult()
    data object NoResult : CardFinishApproveOrderResult()
}
