package com.paypal.android.paypalwebpayments

/**
 * Callback interface for creating a PayPal order.
 *
 * The SDK invokes [createOrder] on a background thread. Implementations should perform
 * their own network call to the merchant server and return the resulting order ID.
 */
fun interface CreateOrderHandler {

    /**
     * Called by the SDK to create an order. Must return a [CreateOrderResponse].
     *
     * This is invoked off the main thread — do NOT update UI here.
     */
    fun createOrder(): CreateOrderResponse
}
