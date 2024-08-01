package com.paypal.android.models

import com.paypal.android.corepayments.OrderIntent

data class OrderRequest(
    val orderIntent: OrderIntent,
    val shouldVault: Boolean = false
)
