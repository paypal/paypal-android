package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

internal sealed class CardStatus  {

// TODO: migrate approve order to use auth challenge launcher pattern internally
//    class CheckoutError(val error: PayPalSDKError) : PayPalWebStatus()
//    class CheckoutSuccess(val result: PayPalWebCheckoutResult) : PayPalWebStatus()
//    class CheckoutCanceled(val orderId: String?) : PayPalWebStatus()

    class VaultError(val error: PayPalSDKError) : CardStatus()
    class VaultSuccess(val result: CardVaultResult) : CardStatus()
    object VaultCanceled : CardStatus()
}
