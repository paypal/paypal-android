package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardVaultAuthResponse {
    data class Success(val result: CardVaultResult.Success) : CardVaultAuthResponse()
    data class Failure(val error: PayPalSDKError) : CardVaultAuthResponse()
    object NoResult : CardVaultAuthResponse()
}