package com.paypal.android.ui.paypal

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.paypalpayments.PayPalUserAction
import com.paypal.android.paypalpayments.PayPalUserIdentity
import com.paypal.android.paypalpayments.PayPalFinishStartResult
import com.paypal.android.paypalpayments.PayPalCheckoutFundingSource
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionState

data class PayPalUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val shouldVaultOption: StoreInVaultOption = StoreInVaultOption.NO,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val userIdentity: PayPalUserIdentity? = null,
    val userAction: PayPalUserAction = PayPalUserAction.PAY_NOW,
    val payPalCheckoutState: ActionState<PayPalFinishStartResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val amount: String = "10.99",
    val paymentMethodOption: PayPalCheckoutFundingSource = PayPalCheckoutFundingSource.PAYPAL,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isPayPalCheckoutSuccessful: Boolean
        get() = payPalCheckoutState is ActionState.Success
}
