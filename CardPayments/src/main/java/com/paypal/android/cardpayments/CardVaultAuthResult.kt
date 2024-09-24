package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardVaultAuthResult {
    data class Success(val setupTokenId: String, val status: String) : CardVaultAuthResult()
    data class Failure(val error: PayPalSDKError) : CardVaultAuthResult()
    object NoResult : CardVaultAuthResult()
}