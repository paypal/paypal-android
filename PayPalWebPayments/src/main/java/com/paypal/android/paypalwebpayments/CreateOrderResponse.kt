package com.paypal.android.paypalwebpayments

/**
 * Result returned by [CreateOrderHandler.createOrder].
 */
sealed class CreateOrderResponse {

    /**
     * Order was created successfully.
     *
     * @property orderId The ID of the order returned by the merchant's server.
     */
    data class Success(val orderId: String) : CreateOrderResponse()

    /**
     * Order creation failed.
     *
     * @property error Description of the failure.
     */
    data class Failure(val error: Exception) : CreateOrderResponse()
}
