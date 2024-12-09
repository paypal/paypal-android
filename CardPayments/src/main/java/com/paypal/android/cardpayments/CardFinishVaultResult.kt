package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardFinishVaultResult {

    data class Success(
        val setupTokenId: String,
        val status: String? = null,
        val didAttemptThreeDSecureAuthentication: Boolean = false
    ) : CardFinishVaultResult()

    data class Failure(val error: PayPalSDKError) : CardFinishVaultResult()

    data object Canceled : CardFinishVaultResult()
    data object NoResult : CardFinishVaultResult()
}
