package com.paypal.android.ui.card

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.paypal.android.ui.card.validation.CardType
import com.paypal.android.ui.card.validation.getCardType
import java.lang.Integer.min

// Ref: https://medium.com/@patilshreyas/filtering-and-modifying-text-input-in-jetpack-compose-way-8f7eeedd958
class CardNumberVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val cardType = getCardType(text.text)
        return TransformedText(
            AnnotatedString(formatCardNumber(text.text, cardType)),
            offsetMapping = CardNumberOffsetMapping.of(cardType)
        )
    }

    private fun formatCardNumber(rawCardNumber: String, cardType: CardType): String {
        var offset = 0
        var result = ""
        for (groupLength in cardType.digitGroupings) {
            if (offset < rawCardNumber.length) {
                if (offset != 0) {
                    // add space between grouped numbers (except for first group)
                    result += " "
                }
            } else {
                // formatting done
                break
            }
            val endOffset = min(rawCardNumber.length, offset + groupLength)
            val digitGroup = rawCardNumber.substring(offset, endOffset)
            result += digitGroup
            offset = endOffset
        }

        if (offset < rawCardNumber.length) {
            // append remainder of ungrouped digits
            result += rawCardNumber.substring(offset)
        }
        return result
    }
}