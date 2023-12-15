package com.paypal.android.ui.approveorder

import androidx.compose.runtime.Immutable
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionButtonState

@Immutable
data class ApproveOrderUiState(
    val createOrderState: ActionButtonState<Order, Exception> = ActionButtonState.Ready,
    val completedOrder: Order? = null,
    val approveOrderResult: CardResult? = null,
    val approveOrderErrorMessage: String? = null,
    val scaOption: SCA = SCA.SCA_ALWAYS,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val shouldVault: StoreInVaultOption = StoreInVaultOption.NO,
    val isApproveOrderLoading: Boolean = false,
    val isCompleteOrderLoading: Boolean = false,
)
