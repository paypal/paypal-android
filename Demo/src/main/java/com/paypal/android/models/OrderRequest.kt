package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.model.PaymentMethodSelected
import com.paypal.android.api.model.UserActionSelected
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val intent: OrderIntent,
    val shouldVaultOnSuccess: Boolean,
    val amount: String = "10.99",
    val paymentMethodSelected: PaymentMethodSelected = PaymentMethodSelected.PAYPAL,
    val userAction: UserActionSelected = UserActionSelected.PAY_NOW,
)
