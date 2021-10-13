package com.paypal.android.card

import com.paypal.android.core.OrderData
import com.paypal.android.core.OrderStatus

data class CardOrder(
    override val orderID: String,
    override val status: OrderStatus
) : OrderData