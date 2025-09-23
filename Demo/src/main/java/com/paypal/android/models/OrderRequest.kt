package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val intent: OrderIntent,
    val shouldVault: Boolean = false
)
