package com.paypal.android.uishared.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable

@Composable
fun IntSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedRange<Int>,
    colors: SliderColors,
    steps: Int,
) {
    Slider(
        value = value.toFloat(),
        valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
        steps = steps,
        colors = colors,
        onValueChange = { onValueChange(it.toInt()) }
    )
}
