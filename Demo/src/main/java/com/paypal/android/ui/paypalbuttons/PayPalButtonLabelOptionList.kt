package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.ui.OptionList

@Composable
fun PayPalButtonLabelOptionList(
    selectedOption: PayPalButtonLabel,
    onSelection: (PayPalButtonLabel) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_label),
        options = PayPalButtonLabel.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(PayPalButtonLabel.valueOf(option))
        }
    )
}
