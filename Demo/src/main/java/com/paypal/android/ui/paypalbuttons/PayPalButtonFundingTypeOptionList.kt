package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import com.paypal.android.ui.OptionList

@Composable
fun PayPalButtonFundingTypeOptionList(
    selectedOption: ButtonFundingType,
    onSelection: (ButtonFundingType) -> Unit
) {
    OptionList(
        title = "Funding Type",
        options = ButtonFundingType.values().map { it.name },
        selectedOption = selectedOption.name,
        onOptionSelected = { option ->
            onSelection(ButtonFundingType.valueOf(option))
        }
    )
}