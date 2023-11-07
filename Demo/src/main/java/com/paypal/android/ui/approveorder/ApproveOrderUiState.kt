package com.paypal.android.ui.approveorder

import androidx.compose.runtime.Immutable
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.cardpayments.CardResult

@Immutable
data class ApproveOrderUiState(
    val createdOrder: Order? = null,
    val completedOrder: Order? = null,
    val approveOrderResult: CardResult? = null,
    val approveOrderErrorMessage: String? = null,
    val scaOption: String = "ALWAYS",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isCreateOrderLoading: Boolean = false,
    val shouldVault: Boolean = false,
    val customerId: String = "",
    val isApproveOrderLoading: Boolean = false,
    val isCompleteOrderLoading: Boolean = false,
)
