package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.ui.OptionList

@Composable
fun PayPalButtonLabelOptionList(
    selectedOption: PayPalButtonLabel,
    onSelection: (PayPalButtonLabel) -> Unit
) {
    OptionList(
        title = "Button Label",
        options = PayPalButtonLabel.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(PayPalButtonLabel.valueOf(option))
        }
    )
}
