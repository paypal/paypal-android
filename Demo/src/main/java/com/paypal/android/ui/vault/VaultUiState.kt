package com.paypal.android.ui.vault

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.cardpayments.VaultResult

data class VaultUiState(
    val setupToken: SetupToken? = null,
    val paymentToken: PaymentToken? = null,
    val isCreateSetupTokenLoading: Boolean = false,
    val isUpdateSetupTokenLoading: Boolean = false,
    val isCreatePaymentTokenLoading: Boolean = false,
    val isCreateOrderLoading: Boolean = false,
    val customerId: String = "",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val vaultResult: VaultResult? = null,
    val orderIntent: OrderIntent = OrderIntent.AUTHORIZE,
    val createdOrder: Order? = null
)
