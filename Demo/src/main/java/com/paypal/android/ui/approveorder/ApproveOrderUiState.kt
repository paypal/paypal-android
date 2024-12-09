package com.paypal.android.ui.approveorder

import androidx.compose.runtime.Immutable
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionState

@Immutable
data class ApproveOrderUiState(
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val approveOrderState: ActionState<OrderInfo, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val scaOption: SCA = SCA.SCA_ALWAYS,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val shouldVault: StoreInVaultOption = StoreInVaultOption.NO,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isApproveOrderSuccessful: Boolean
        get() = approveOrderState is ActionState.Success
}
