package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent

data class OrderRequest(
    val orderIntent: OrderIntent,
    val shouldVault: Boolean = false
)
