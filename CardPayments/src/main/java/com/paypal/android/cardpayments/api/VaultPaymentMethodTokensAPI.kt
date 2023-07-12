package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.VaultResult

internal class VaultPaymentMethodTokensAPI {

    fun createSetupToken(vaultRequest: VaultRequest): VaultResult {
        return VaultResult(setupTokenId = "fake-setup-token-id")
    }
}