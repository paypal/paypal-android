package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PaymentButtonSize
import com.paypal.android.uishared.components.OptionList

@Composable
fun PaymentButtonSizeOptionList(
    selectedOption: PaymentButtonSize,
    onSelection: (PaymentButtonSize) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_size),
        options = PaymentButtonSize.values().map { it.name },
        selectedOption = selectedOption.name,
        onSelectedOptionChange = { option ->
            onSelection(PaymentButtonSize.valueOf(option))
        }
    )
}
