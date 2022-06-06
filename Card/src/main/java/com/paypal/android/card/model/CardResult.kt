package com.paypal.android.card.model

import android.net.Uri
import com.paypal.android.core.OrderStatus

/**
 * A result returned by [CardClient] when an order was successfully approved with a [Card].
 */
data class CardResult(
    val orderID: String,
    val status: OrderStatus? = null,
    val paymentSource: PaymentSource? = null,
    val deepLinkUrl: Uri? = null
)
