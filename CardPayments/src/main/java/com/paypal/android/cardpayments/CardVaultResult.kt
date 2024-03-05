package com.paypal.android.cardpayments

/**
 * @suppress
 *
 * A result returned by [CardClient] when an a successful vault occurs.
 *
 * @param setupTokenId the id for the setup token that was recently updated
 * @param status the status of the updated setup token
 */
data class CardVaultResult(
    val setupTokenId: String,
    val status: String,
    // NOTE: This technically needs to null by default to prevent a breaking change
    // NEXT MAJOR VERSION: make `CardVaultResult` constructor private
    val authChallenge: CardAuthChallenge? = null
)
