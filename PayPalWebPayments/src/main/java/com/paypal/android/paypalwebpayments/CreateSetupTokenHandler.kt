package com.paypal.android.paypalwebpayments

/**
 * Callback interface for merchant-side setup-token creation, invoked by
 * [PayPalWebCheckoutClient.vault] in parallel with the Shopper Session API call.
 *
 * The SDK passes a completion [callback] to [createSetupToken]. Implementations must call
 * [callback] exactly once with the result. Use your own coroutine scope (e.g.
 * `viewModelScope`) or any async mechanism — do **not** block the calling thread.
 *
 * Example (Kotlin lambda using `viewModelScope`):
 * ```kotlin
 * paypalClient.vault(activity, request, createSetupToken = { callback ->
 *     viewModelScope.launch {
 *         try {
 *             val setupTokenId = myServer.createSetupToken()
 *             callback(CreateSetupTokenResponse.Success(setupTokenId))
 *         } catch (e: Exception) {
 *             callback(CreateSetupTokenResponse.Failure(e))
 *         }
 *     }
 * }) { vaultResult -> ... }
 * ```
 */
fun interface CreateSetupTokenHandler {
    /**
     * Create a PayPal setup token asynchronously and report the result via [callback].
     *
     * Must call [callback] exactly once:
     * - `callback(CreateSetupTokenResponse.Success(setupTokenId))` on success
     * - `callback(CreateSetupTokenResponse.Failure(error))` on failure
     *
     * @param callback SDK-provided completion handler; must be called exactly once.
     */
    fun createSetupToken(callback: (CreateSetupTokenResponse) -> Unit)
}

/**
 * Result of a merchant-provided setup-token creation callback passed to
 * [PayPalWebCheckoutClient.vault].
 */
sealed class CreateSetupTokenResponse {
    /**
     * @property setupTokenId The setup token ID returned by the merchant's server.
     */
    data class Success(val setupTokenId: String) : CreateSetupTokenResponse()

    /**
     * @property error The exception describing the failure.
     */
    data class Failure(val error: Exception) : CreateSetupTokenResponse()
}
