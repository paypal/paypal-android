package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardStatus {

    class VaultError(val error: PayPalSDKError) : CardStatus()
    class VaultSuccess(val result: CardVaultResult) : CardStatus()
    class VaultCanceled(val setupTokenId: String?) : CardStatus()

    class UnknownError(val error: Throwable) : CardStatus()
    data object NoResult : CardStatus()
}
