package com.paypal.android.ui.paypalbuttons

import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.paymentbuttons.PaymentButtonSize

data class PayPalButtonsUiState(
    val fundingType: ButtonFundingType = ButtonFundingType.PAYPAL,
    val payPalCreditButtonColor: PayPalCreditButtonColor = PayPalCreditButtonColor.DARK_BLUE,
    val payPalButtonColor: PayPalButtonColor = PayPalButtonColor.GOLD,
    val payPalButtonLabel: PayPalButtonLabel = PayPalButtonLabel.PAYPAL,
    val paymentButtonShape: PaymentButtonShape = PaymentButtonShape.ROUNDED,
    val paymentButtonSize: PaymentButtonSize = PaymentButtonSize.SMALL,
    val customCornerRadius: Int? = null
)
