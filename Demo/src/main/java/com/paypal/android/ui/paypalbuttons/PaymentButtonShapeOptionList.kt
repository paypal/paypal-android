package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.uishared.components.OptionList

@Composable
fun PaymentButtonShapeOptionList(
    selectedOption: PaymentButtonShape,
    onSelection: (PaymentButtonShape) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_shape),
        options = PaymentButtonShape.values().map { it.name },
        selectedOption = selectedOption.name,
        onSelectedOptionChange = { option ->
            onSelection(PaymentButtonShape.valueOf(option))
        }
    )
}
