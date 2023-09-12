package com.paypal.android.cardpayments.model

import android.net.Uri

/**
 * A result returned by [CardClient] when an order was successfully approved with a [Card].
 *
 * @property [orderId] associated order ID.
 */
data class CardResult(
    val orderId: String,

    /**
     * @suppress
     */
    val deepLinkUrl: Uri? = null
)
