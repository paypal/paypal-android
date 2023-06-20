package com.paypal.android.ui.card

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ExpirationDateVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        var mode = ExpirationDateOffsetMapping.Mode.DEFAULT
        if (text.isNotEmpty()) {
            val firstChar = text[0]
            if (firstChar != '0' && firstChar != '1') {
                mode = ExpirationDateOffsetMapping.Mode.LEADING_ZERO
            }
        }

        return TransformedText(
            AnnotatedString(formatDate(text.text)),
            offsetMapping = ExpirationDateOffsetMapping(mode)
        )
    }

    private fun formatDate(rawDate: String): String {
        var result = ""

        val dateComponents = if (rawDate.length > 1) {
            listOf(rawDate.substring(0, 2), rawDate.substring(2))
        } else {
            listOf(rawDate)
        }

        if(dateComponents.isNotEmpty()) {
            val month = dateComponents[0]
            result += if (month.length == 1) {
                val firstMonthChar = month[0]
                if (firstMonthChar == '0' || firstMonthChar == '1') {
                    // passthrough
                    month
                } else {
                    // prepend zero
                    "0${month}"
                }
            } else {
                // passthrough
                month
            }

            if (result.length == 2) {
                result += "/"
            }

            if (dateComponents.size > 1) {
                // append remainder
                result += dateComponents[1]
            }
        }
        return result
    }
}