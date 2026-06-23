package com.paypal.android.paypalwebpayments

/**
 * Callback interface for creating a PayPal setup token (vault flow).
 *
 * The SDK invokes [createSetupToken] on a background thread. Implementations should perform
 * their own network call to the merchant server and return the resulting setup token ID.
 */
fun interface CreateSetupTokenHandler {

    /**
     * Called by the SDK to create a setup token. Must return a [CreateSetupTokenResponse].
     *
     * This is invoked off the main thread — do NOT update UI here.
     */
    fun createSetupToken(): CreateSetupTokenResponse
}
