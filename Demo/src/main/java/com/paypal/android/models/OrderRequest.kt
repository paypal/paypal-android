package com.paypal.android.models

import com.paypal.android.cardpayments.OrderIntent

data class OrderRequest(
    val orderIntent: OrderIntent,
    val shouldVault: Boolean = false,
    val vaultCustomerId: String = ""
)
