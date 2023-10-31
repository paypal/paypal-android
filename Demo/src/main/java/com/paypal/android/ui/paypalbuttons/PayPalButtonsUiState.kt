package com.paypal.android.ui.paypalbuttons

import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor

data class PayPalButtonsUiState(
    val fundingType: ButtonFundingType = ButtonFundingType.PAYPAL,
    val payPalCreditButtonColor: PayPalCreditButtonColor = PayPalCreditButtonColor.DARK_BLUE,
    val payPalButtonColor: PayPalButtonColor = PayPalButtonColor.GOLD,
    val payPalButtonLabel: PayPalButtonLabel = PayPalButtonLabel.PAYPAL
) {
}
