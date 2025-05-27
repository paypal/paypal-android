package com.paypal.android.ui.paypalbuttons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.paypal.android.R
import com.paypal.android.paymentbuttons.PaymentButtonEdges
import com.paypal.android.uishared.components.IntSlider
import com.paypal.android.uishared.components.OptionListItem
import com.paypal.android.uishared.components.OptionListTitle
import com.paypal.android.utils.UIConstants

@Composable
fun PaymentButtonEdgesOptionList(
    edges: PaymentButtonEdges,
    onSelection: (PaymentButtonEdges) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
    ) {
        OptionListTitle(text = stringResource(id = R.string.pay_pal_button_edges))
        Column(
            modifier = Modifier.selectableGroup()
        ) {
            OptionListItem(
                text = "SOFT",
                isSelected = (edges is PaymentButtonEdges.Soft),
                onClick = { onSelection(PaymentButtonEdges.Soft) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
            OptionListItem(
                text = "PILL",
                isSelected = (edges is PaymentButtonEdges.Pill),
                onClick = { onSelection(PaymentButtonEdges.Pill) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
            OptionListItem(
                text = "SHARP",
                isSelected = (edges is PaymentButtonEdges.Sharp),
                onClick = { onSelection(PaymentButtonEdges.Sharp) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = UIConstants.paddingMedium))
            OptionListItem(
                text = "CUSTOM",
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
            PaymentButtonEdgesCustomRadiusSlider(edges, onSelection = onSelection)
        }
    }
}

@Composable
fun PaymentButtonEdgesCustomRadiusSlider(
    edges: PaymentButtonEdges,
    onSelection: (PaymentButtonEdges) -> Unit
) {
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
