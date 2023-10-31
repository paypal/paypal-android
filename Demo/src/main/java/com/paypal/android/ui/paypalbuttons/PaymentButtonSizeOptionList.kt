package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import com.paypal.android.paymentbuttons.PaymentButtonSize
import com.paypal.android.ui.OptionList

@Composable
fun PaymentButtonSizeOptionList(
    selectedOption: PaymentButtonSize,
    onSelection: (PaymentButtonSize) -> Unit
) {
    OptionList(
        title = "Button Size",
        options = PaymentButtonSize.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(PaymentButtonSize.valueOf(option))
        }
    )
}
