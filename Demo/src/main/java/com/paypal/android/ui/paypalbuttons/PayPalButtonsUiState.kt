package com.paypal.android.ui.paypalbuttons

import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.paymentbuttons.PaymentButtonEdges

data class PayPalButtonsUiState(
    val fundingType: ButtonFundingType = ButtonFundingType.PAYPAL,
    val payPalCreditButtonColor: PayPalCreditButtonColor = PayPalCreditButtonColor.BLUE,
    val payPalButtonColor: PayPalButtonColor = PayPalButtonColor.BLUE,
    val payPalButtonLabel: PayPalButtonLabel = PayPalButtonLabel.NONE,
    val paymentButtonEdges: PaymentButtonEdges = PaymentButtonEdges.Soft,
)
