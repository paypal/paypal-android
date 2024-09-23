package com.paypal.android.cardpayments

import com.paypal.android.corepayments.PayPalSDKError

/**
 * @suppress
 *
 * A result returned by [CardClient] when an a successful vault occurs.
 *
 * @param setupTokenId the id for the setup token that was recently updated
 * @param status the status of the updated setup token
 * @param authChallenge an authentication challenge; will be non-null when 3DS authentication
 * is required to complete a vault
 */
// NEXT MAJOR VERSION: make `CardVaultResult` constructor private
sealed class CardVaultResult {

    data class Success(val setupTokenId: String, val status: String) : CardVaultResult()

    data class AuthorizationRequired(val authChallenge: CardAuthChallenge) : CardVaultResult()

    data class Failure(val error: PayPalSDKError) : CardVaultResult()
}

