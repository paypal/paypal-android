package com.paypal.android.cardpayments

/**
 * A result returned by [CardClient] when an order was successfully approved with a [Card].
 *
 * @property [orderId] associated order ID.
 * @property [status] status of the order
 * @property [didAttemptThreeDSecureAuthentication] 3DS verification was attempted.
 * Use v2/checkout/orders/{orderId} in your server to get verification results.
 */
data class CardResult internal constructor(
    val orderId: String,
    val status: String? = null,
    val didAttemptThreeDSecureAuthentication: Boolean = false
)
