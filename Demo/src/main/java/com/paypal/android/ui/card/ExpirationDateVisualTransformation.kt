package com.paypal.android.ui.card

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ExpirationDateVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(formatDate(text.text)),
            offsetMapping = ExpirationDateOffsetMapping()
        )
    }

    private fun formatDate(rawDate: String): String {
        return rawDate
    }
}