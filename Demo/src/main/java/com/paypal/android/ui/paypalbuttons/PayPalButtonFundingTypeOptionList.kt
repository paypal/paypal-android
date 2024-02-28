package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.uishared.components.OptionList

@Composable
fun PayPalButtonFundingTypeOptionList(
    selectedOption: ButtonFundingType,
    onSelection: (ButtonFundingType) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_type),
        options = ButtonFundingType.values().map { it.name },
        selectedOption = selectedOption.name,
        onSelectedOptionChange = { option ->
            onSelection(ButtonFundingType.valueOf(option))
        }
    )
}
