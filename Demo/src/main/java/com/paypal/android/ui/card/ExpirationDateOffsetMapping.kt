package com.paypal.android.ui.card

import androidx.compose.ui.text.input.OffsetMapping

class ExpirationDateOffsetMapping(private val mode: Mode) : OffsetMapping {

    enum class Mode {
        DEFAULT, LEADING_ZERO
    }

    override fun originalToTransformed(offset: Int): Int {
        var transformed = offset
        if (mode == Mode.LEADING_ZERO) {
            transformed++
        }

        if (transformed >= 2) {
            // increment for added slash in date
            transformed++
        }
        return transformed
    }

    override fun transformedToOriginal(offset: Int): Int {
        var transformed = offset
        if (transformed >= 3) {
            // decrement for added slash in date
            transformed--
        }
        if (mode == Mode.LEADING_ZERO) {
            transformed--
        }
        return transformed
    }
}