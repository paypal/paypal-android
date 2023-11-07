package com.paypal.android.ui.approveorder

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class DateVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val dateString = DateString(text.text)
        return TransformedText(
            AnnotatedString(formatDate(dateString)),
            offsetMapping = DateOffsetMapping(dateString)
        )
    }

    private fun formatDate(dateString: DateString): String {
        return dateString.formatted
    }

    class DateOffsetMapping(private val dateString: DateString) : OffsetMapping {

        override fun originalToTransformed(offset: Int): Int {
            return dateString.mapRawOffsetToFormatted(offset)
        }

        override fun transformedToOriginal(offset: Int): Int {
            return dateString.mapFormattedOffsetToRawOffset(offset)
        }
    }
}
