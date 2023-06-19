package com.paypal.android.ui.card

import androidx.compose.ui.text.input.OffsetMapping

class ExpirationDateOffsetMapping : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return offset
    }

    override fun transformedToOriginal(offset: Int): Int {
        return offset
    }
}