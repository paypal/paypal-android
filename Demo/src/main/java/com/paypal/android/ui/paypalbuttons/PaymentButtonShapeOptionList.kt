package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.ui.OptionList

@Composable
fun PaymentButtonShapeOptionList(
    selectedOption: PaymentButtonShape,
    onSelection: (PaymentButtonShape) -> Unit
) {
    OptionList(
        title = "Button Shape",
        options = PaymentButtonShape.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(PaymentButtonShape.valueOf(option))
        }
    )
}