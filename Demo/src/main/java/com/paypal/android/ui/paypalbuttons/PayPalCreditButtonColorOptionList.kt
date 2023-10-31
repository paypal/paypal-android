package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.ui.OptionList

@Composable
fun PayPalCreditButtonColorOptionList(
    selectedOption: PayPalCreditButtonColor,
    onSelection: (PayPalCreditButtonColor) -> Unit
) {
    OptionList(
        title = "Button Color",
        options = PayPalCreditButtonColor.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(PayPalCreditButtonColor.valueOf(option))
        }
    )
}
