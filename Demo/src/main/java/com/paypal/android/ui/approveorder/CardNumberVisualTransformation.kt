package com.paypal.android.ui.approveorder

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.lang.Integer.max
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

    class CardNumberOffsetMapping private constructor(private val cardType: CardType) : OffsetMapping {

        companion object {
            private val amexMapping = CardNumberOffsetMapping(CardType.AMERICAN_EXPRESS)
            private val visaMapping = CardNumberOffsetMapping(CardType.VISA)
            private val unknownMapping = CardNumberOffsetMapping(CardType.UNKNOWN_CARD)

            fun of(cardType: CardType) = when (cardType) {
                CardType.AMERICAN_EXPRESS -> amexMapping
                CardType.VISA -> visaMapping
                CardType.UNKNOWN_CARD -> unknownMapping
            }
        }

        override fun originalToTransformed(offset: Int): Int {
            var completeGroups = 0
            var transformOffset = 0

            // exclude last group
            for (groupLength in cardType.allDigitGroupingsExceptLast) {
                transformOffset += groupLength
                if (transformOffset < offset) {
                    completeGroups += 1
                } else {
                    // done
                    break
                }
            }
            return offset + completeGroups
        }

        override fun transformedToOriginal(offset: Int): Int {
            var completeGroups = 0
            var transformOffset = 0
            for (groupLength in cardType.allDigitGroupingsExceptLast) {
                transformOffset += groupLength
                if (transformOffset < offset) {
                    completeGroups += 1
                } else {
                    // done
                    break
                }
            }
            // subtract a space for every complete group (clamp to zero)
            return max(0, offset - completeGroups)
        }
    }
}
