package com.paypal.android.cardpayments

import android.net.Uri

/**
 * A result returned by [CardClient] when an order was successfully approved with a [Card].
 *
 * @property [orderId] associated order ID.
 * @property [liabilityShift] Liability shift value returned from 3DS verification
 */
data class CardResult(
    val orderId: String,

    /**
     * @suppress
     */
    val deepLinkUrl: Uri? = null,
    val liabilityShift: String? = null
)
