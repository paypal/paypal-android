package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.ui.OptionList

@Composable
fun PayPalButtonColorOptionList(
    selectedOption: PayPalButtonColor,
    onSelection: (PayPalButtonColor) -> Unit
) {
    OptionList(
        title = "Button Color",
        options = PayPalButtonColor.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(PayPalButtonColor.valueOf(option))
        }
    )
}
