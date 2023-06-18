package com.paypal.android.ui.card

import androidx.compose.ui.text.input.OffsetMapping
import com.paypal.android.ui.card.validation.CardType
import java.lang.Integer.max
import java.lang.Integer.min

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
