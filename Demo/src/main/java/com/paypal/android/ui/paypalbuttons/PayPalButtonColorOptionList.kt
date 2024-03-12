package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.uishared.components.OptionList

@Composable
fun PayPalButtonColorOptionList(
    selectedOption: PayPalButtonColor,
    onSelection: (PayPalButtonColor) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_color),
        options = PayPalButtonColor.values().map { it.name },
        selectedOption = selectedOption.name,
        onSelectedOptionChange = { option ->
            onSelection(PayPalButtonColor.valueOf(option))
        }
    )
}
