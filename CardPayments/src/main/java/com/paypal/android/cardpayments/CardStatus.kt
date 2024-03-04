package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

internal sealed class CardStatus  {

    class ApproveOrderError(val error: PayPalSDKError) : CardStatus()
    class ApproveOrderSuccess(val result: CardResult) : CardStatus()
    class ApproveOrderCanceled(val orderId: String?) : CardStatus()

    class VaultError(val error: PayPalSDKError) : CardStatus()
    class VaultSuccess(val result: CardVaultResult) : CardStatus()
    object VaultCanceled : CardStatus()
}
