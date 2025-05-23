package com.paypal.android.uishared.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun IntSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedRange<Int>,
    colors: SliderColors,
    steps: Int,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value.toFloat(),
        valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
        steps = steps,
        colors = colors,
        onValueChange = { onValueChange(it.toInt()) },
        modifier = modifier
    )
}
