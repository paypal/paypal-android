package com.paypal.android.venmo

import com.paypal.android.corepayments.PayPalSDKError

sealed class VenmoFinishVaultResult {
    data class Success(
        val approvalTokenId: String,
        val approvalSessionId: String?
    ) : VenmoFinishVaultResult()

    data class Canceled(
        val setupTokenId: String? = null
    ) : VenmoFinishVaultResult()

    data object NoResult : VenmoFinishVaultResult()

    data class Failure(val error: PayPalSDKError) : VenmoFinishVaultResult()
}
