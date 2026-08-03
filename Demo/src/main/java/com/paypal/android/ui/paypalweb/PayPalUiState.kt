package com.paypal.android.ui.paypalweb

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.paypalwebpayments.PayPalUserAction
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionState

data class PayPalUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val shouldVaultOption: StoreInVaultOption = StoreInVaultOption.NO,
    val createOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val userIdentity: PayPalUserIdentity? = null,
    val userAction: PayPalUserAction = PayPalUserAction.PAY_NOW,
    val payPalWebCheckoutState: ActionState<PayPalWebCheckoutFinishStartResult.Success, Exception> = ActionState.Idle,
    val completeOrderState: ActionState<Order, Exception> = ActionState.Idle,
    val amount: String = "10.99",
    val paymentMethodOption: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL,
) {
    val isCreateOrderSuccessful: Boolean
        get() = createOrderState is ActionState.Success

    val isPayPalWebCheckoutSuccessful: Boolean
        get() = payPalWebCheckoutState is ActionState.Success
}
