package com.paypal.android.paypalwebpayments

/**
 * Result returned by [CreateSetupTokenHandler.createSetupToken].
 */
sealed class CreateSetupTokenResponse {

    /**
     * Setup token was created successfully.
     *
     * @property setupTokenId The ID of the setup token returned by the merchant's server.
     */
    data class Success(val setupTokenId: String) : CreateSetupTokenResponse()

    /**
     * Setup token creation failed.
     *
     * @property error Description of the failure.
     */
    data class Failure(val error: Exception) : CreateSetupTokenResponse()
}
