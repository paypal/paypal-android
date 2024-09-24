package com.paypal.android.ui.approveorder

import androidx.compose.runtime.Immutable
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.cardpayments.CardApproveOrderAuthResult
import com.paypal.android.cardpayments.CardApproveOrderResult
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.CardVaultAuthResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionState

@Immutable
data class ApproveOrderUiState(
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val approveOrderState: ActionState<CardApproveOrderResult.Success, Exception> = ActionState.Idle,
    val authChallengeState: ActionState<CardApproveOrderAuthResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val scaOption: SCA = SCA.SCA_ALWAYS,
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val shouldVault: StoreInVaultOption = StoreInVaultOption.NO,
    val authChallenge: CardAuthChallenge? = null
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isApproveOrderSuccessful: Boolean
        get() = approveOrderState is ActionState.Success

    val isApproveOrderWith3DSSuccessful: Boolean
        get() = authChallengeState is ActionState.Success
}
