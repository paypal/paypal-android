package com.paypal.android.ui.card.validation

@Suppress("MagicNumber")
enum class CardType(
    val cardNumberIndices: List<Int>,
    val maxCardLength: Int
) {
    AMERICAN_EXPRESS(
        cardNumberIndices = listOf(4, 11),
        maxCardLength = 15
    ),
    VISA(
        cardNumberIndices = listOf(4, 9, 14),
        maxCardLength = 16
    ),
    UNKNOWN_CARD(
        cardNumberIndices = listOf(4, 9, 14),
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
