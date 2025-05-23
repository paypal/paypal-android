package com.paypal.android.ui.paypalbuttons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import com.paypal.android.paymentbuttons.PaymentButtonEdges
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.uishared.components.IntSlider
import com.paypal.android.uishared.components.OptionListItem
import com.paypal.android.uishared.components.OptionListTitle
import com.paypal.android.utils.UIConstants
import kotlin.reflect.KClass

private enum class PaymentButtonEdgesOption {
    SOFT, PILL, SHARP, CUSTOM
}

@Composable
fun PaymentButtonEdgesOptionList(
    edges: PaymentButtonEdges,
    onSelection: (PaymentButtonEdges) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
    ) {
        OptionListTitle(text = stringResource(id = R.string.pay_pal_button_shape))
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            OptionListItem(
                text = edgesToString(PaymentButtonEdges.Soft),
                isSelected = (edges is PaymentButtonEdges.Soft),
                onClick = { onSelection(PaymentButtonEdges.Soft) }
            )
            Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
            OptionListItem(
                text = edgesToString(PaymentButtonEdges.Pill),
                isSelected = (edges is PaymentButtonEdges.Pill),
                onClick = { onSelection(PaymentButtonEdges.Pill) }
            )
            Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
            OptionListItem(
                text = edgesToString(PaymentButtonEdges.Sharp),
                isSelected = (edges is PaymentButtonEdges.Sharp),
                onClick = { onSelection(PaymentButtonEdges.Sharp) }
            )
            Divider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
            OptionListItem(
                text = edgesToString(PaymentButtonEdges.Custom(0f)),
                isSelected = (edges is PaymentButtonEdges.Custom),
                onClick = {
                    val selectionHasChanged = (edges !is PaymentButtonEdges.Custom)
                    if (selectionHasChanged) {
                        // to prevent the overwrite of an existing custom radius slider value, only
                        // update if a selection change has occurred
                        onSelection(PaymentButtonEdges.Custom(0f))
                    }
                }
            )
            Column(
                modifier = Modifier.padding(
                    start = UIConstants.paddingMedium,
                    end = UIConstants.paddingMedium,
                    bottom = UIConstants.paddingMedium
                )
            ) {
                val cornerRadius = (edges as? PaymentButtonEdges.Custom)?.cornerRadius ?: 0
                Text(
                    text = "${cornerRadius.toInt()}px",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(bottom = UIConstants.paddingSmall)
                        .fillMaxWidth()
                )
                IntSlider(
                    value = cornerRadius.toInt(),
                    valueRange = 0..CORNER_RADIUS_SLIDER_MAX,
                    steps = CORNER_RADIUS_SLIDER_MAX,
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    onValueChange = { value ->
                        val newSelection = PaymentButtonEdges.Custom(value.toFloat())
                        onSelection(newSelection)
                    }
                )

            }
        }
    }
}

fun edgesToString(edges: PaymentButtonEdges): String {
    val option = when (edges) {
        PaymentButtonEdges.Pill -> PaymentButtonEdgesOption.PILL
        PaymentButtonEdges.Sharp -> PaymentButtonEdgesOption.SHARP
        PaymentButtonEdges.Soft -> PaymentButtonEdgesOption.SOFT
        is PaymentButtonEdges.Custom -> PaymentButtonEdgesOption.CUSTOM
    }
    return option.name
}

//fun stringToEdges(value: String): PaymentButtonEdges {
//    val edgesOption = PaymentButtonEdgesOption.valueOf(value)
//    return when (edgesOption) {
//        PaymentButtonEdgesOption.SOFT -> PaymentButtonEdges.Soft
//        PaymentButtonEdgesOption.PILL -> PaymentButtonEdges.Pill
//        PaymentButtonEdgesOption.SHARP -> PaymentButtonEdges.Sharp
//    }
//}
