package com.paypal.android.cardpayments.api

import com.paypal.android.corepayments.OrderStatus

internal data class CreateOrderResponse(val orderId: String, val status: OrderStatus)
