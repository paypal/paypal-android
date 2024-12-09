package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

sealed class CardVaultResult {

    /**
     * A result returned by [CardClient] when an a successful vault occurs.
     *
     * @param setupTokenId the id for the setup token that was recently updated
     * @param status the status of the updated setup token
     * @property [didAttemptThreeDSecureAuthentication] 3DS verification was attempted.
     * Use v2/checkout/orders/{orderId} in your server to get verification results.
     */
    data class Success(
        val setupTokenId: String,
        val status: String? = null,
        val didAttemptThreeDSecureAuthentication: Boolean = false
    ) : CardVaultResult()

    data class AuthorizationRequired(val authChallenge: CardAuthChallenge) : CardVaultResult()
    data class Failure(val error: PayPalSDKError) : CardVaultResult()
}
