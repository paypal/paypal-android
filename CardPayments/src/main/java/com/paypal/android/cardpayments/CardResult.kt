package com.paypal.android.cardpayments

import android.net.Uri

/**
 * A result returned by [CardClient] when an order was successfully approved with a [Card].
 *
 * @property [orderId] associated order ID.
 * @property [liabilityShift] Liability shift value returned from 3DS verification
 * @property [status] status of the order
 * @property [didAttemptThreeDSecureAuthentication] 3DS verification was attempted.
 * Use v2/checkout/orders/{orderId} in your server to get verification results.
 */
data class CardResult(
    val orderId: String,

    /**
     * @suppress
     */
    @Deprecated("Use status instead.")
    val deepLinkUrl: Uri? = null,

    @Deprecated("Use didAttemptThreeDSecureAuthentication instead.")
    val liabilityShift: String? = null,

    val status: String? = null,

    val didAttemptThreeDSecureAuthentication: Boolean = false
)
