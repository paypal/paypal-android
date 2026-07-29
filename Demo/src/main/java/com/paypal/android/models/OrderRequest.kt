package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val intent: OrderIntent,
    val shouldVaultOnSuccess: Boolean,
    val amount: String = "10.99",
    val paymentMethodSelected: String = "PAYPAL_CREDIT",
    val userAction: String = "PAY_NOW",
)
