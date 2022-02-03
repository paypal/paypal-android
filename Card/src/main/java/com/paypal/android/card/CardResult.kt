package com.paypal.android.card

import com.paypal.android.core.OrderStatus

data class CardResult(val orderID: String, val status: OrderStatus)
