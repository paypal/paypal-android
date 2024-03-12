package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.uishared.components.OptionList

@Composable
fun PayPalCreditButtonColorOptionList(
    selectedOption: PayPalCreditButtonColor,
    onSelection: (PayPalCreditButtonColor) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_color),
        options = PayPalCreditButtonColor.values().map { it.name },
        selectedOption = selectedOption.name,
        onSelectedOptionChange = { option ->
            onSelection(PayPalCreditButtonColor.valueOf(option))
        }
    )
}
