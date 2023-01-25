package com.paypal.android.cardpayments.api

import com.paypal.android.corepayments.OrderStatus

internal data class CreateOrderResponse(val orderID: String, val status: OrderStatus)
