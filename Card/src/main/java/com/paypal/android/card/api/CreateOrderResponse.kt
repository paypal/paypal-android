package com.paypal.android.card.api

import com.paypal.android.core.OrderStatus

internal class CreateOrderResponse(val orderID: String, val status: OrderStatus)