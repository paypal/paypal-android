package com.paypal.android.paypalwebpayments

/**
 * Callback interface for merchant-side order creation, invoked by
 * [PayPalWebCheckoutClient.start] in parallel with the Shopper Session API call.
 *
 * The SDK passes a completion [callback] to [createOrder]. Implementations must call
 * [callback] exactly once with the result. Use your own coroutine scope (e.g.
 * `viewModelScope`) or any async mechanism — do **not** block the calling thread.
 *
 * Example (Kotlin lambda using `viewModelScope`):
 * ```kotlin
 * paypalClient.start(activity, request, createOrder = { callback ->
 *     viewModelScope.launch {
 *         try {
 *             val orderId = myServer.createOrder()
 *             callback(CreateOrderResponse.Success(orderId))
 *         } catch (e: Exception) {
 *             callback(CreateOrderResponse.Failure(e))
 *         }
 *     }
 * }) { startResult -> ... }
 * ```
 */
fun interface CreateOrderHandler {
    /**
     * Create a PayPal order asynchronously and report the result via [callback].
     *
     * Must call [callback] exactly once:
     * - `callback(CreateOrderResponse.Success(orderId))` on success
     * - `callback(CreateOrderResponse.Failure(error))` on failure
     *
     * @param callback SDK-provided completion handler; must be called exactly once.
     */
    fun createOrder(callback: (CreateOrderResponse) -> Unit)
}

sealed class CreateOrderResponse {
    /**
     * @property orderId The order ID returned by the merchant's server.
     */
    data class Success(val orderId: String) : CreateOrderResponse()

    /**
     * @property error The exception describing the failure.
     */
    data class Failure(val error: Exception) : CreateOrderResponse()
}
