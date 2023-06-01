package com.paypal.android.cardpayments.model

import android.net.Uri

/**
 * A result returned by [CardClient] when an order was successfully approved with a [Card].
 */
data class CardResult(
    val orderID: String,
    val deepLinkUrl: Uri? = null
)
