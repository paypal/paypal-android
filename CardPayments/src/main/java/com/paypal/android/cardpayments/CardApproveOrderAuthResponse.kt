package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardApproveOrderAuthResponse {

    data class Success(val result: CardApproveOrderResult.Success) :
        CardApproveOrderAuthResponse()

    data class Failure(val error: PayPalSDKError, val orderId: String? = null) : CardApproveOrderAuthResponse()
    object NoResult : CardApproveOrderAuthResponse()
}