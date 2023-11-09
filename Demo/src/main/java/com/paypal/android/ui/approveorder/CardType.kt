package com.paypal.android.ui.approveorder

import java.lang.Integer.max

@Suppress("MagicNumber")
enum class CardType(
    val cardNumberIndices: List<Int>,
    val digitGroupings: List<Int>,
    val maxCardLength: Int,
    val allDigitGroupingsExceptLast: List<Int> = digitGroupings.slice(
        0 until max(
            0,
            digitGroupings.size - 1
        )
    )
) {
    // Ref: https://stackoverflow.com/a/65726499

    AMERICAN_EXPRESS(
        cardNumberIndices = listOf(4, 11),
        digitGroupings = listOf(4, 6, 5),
        maxCardLength = 15
    ),
    VISA(
        cardNumberIndices = listOf(4, 9, 14),
        digitGroupings = listOf(4, 4, 4, 4),
        maxCardLength = 16
    ),
    UNKNOWN_CARD(
        cardNumberIndices = listOf(4, 9, 14),
        digitGroupings = emptyList(),
        maxCardLength = 16
    )
}

fun getCardType(cardNumber: String): CardType {
    return when {
        cardNumber.startsWith("34") || cardNumber.startsWith("37") -> {
            CardType.AMERICAN_EXPRESS
        }

        cardNumber.startsWith("4") -> CardType.VISA
        else -> CardType.UNKNOWN_CARD
    }
}
