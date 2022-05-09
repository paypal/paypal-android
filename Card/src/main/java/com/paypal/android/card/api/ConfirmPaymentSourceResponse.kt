package com.paypal.android.card.api

import com.paypal.android.core.OrderStatus

internal data class ConfirmPaymentSourceResponse(val orderId: String, val status: OrderStatus, val payerActionHref: String? = null)