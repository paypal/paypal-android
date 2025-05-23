package com.paypal.android.ui.paypalbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PaymentButtonEdges
import com.paypal.android.uishared.components.OptionList

private enum class PaymentButtonEdgesOption {
    SOFT, PILL, SHARP
}

@Composable
fun PaymentButtonEdgesOptionList(
    edges: PaymentButtonEdges,
    onSelection: (PaymentButtonEdges) -> Unit
) {
    OptionList(
        title = stringResource(id = R.string.pay_pal_button_shape),
        options = PaymentButtonEdgesOption.entries.map { it.name },
        selectedOption = edgesToString(edges),
        onSelectedOptionChange = { option ->
            onSelection(stringToEdges(option))
        }
    )
}

fun edgesToString(edges: PaymentButtonEdges): String {
    val option = when (edges) {
        PaymentButtonEdges.Pill -> PaymentButtonEdgesOption.PILL
        PaymentButtonEdges.Sharp -> PaymentButtonEdgesOption.SHARP
        PaymentButtonEdges.Soft -> PaymentButtonEdgesOption.SOFT
        is PaymentButtonEdges.Custom -> TODO("Should not get here. Figure out the best way to handle this scenario")
    }
    return option.name
}

fun stringToEdges(value: String): PaymentButtonEdges {
    val edgesOption = PaymentButtonEdgesOption.valueOf(value)
    return when (edgesOption) {
        PaymentButtonEdgesOption.SOFT -> PaymentButtonEdges.Soft
        PaymentButtonEdgesOption.PILL -> PaymentButtonEdges.Pill
        PaymentButtonEdgesOption.SHARP -> PaymentButtonEdges.Sharp
    }
}
