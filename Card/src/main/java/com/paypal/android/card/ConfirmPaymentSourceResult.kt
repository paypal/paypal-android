package com.paypal.android.card

import com.paypal.android.core.OrderData
import com.paypal.android.core.OrderError

data class ConfirmPaymentSourceResult(
    val response: OrderData? = null,
    val error: OrderError? = null
)
