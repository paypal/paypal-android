package com.paypal.android.ui.approveorder

object CardFormatter {

    /**
     * Formats a card number string.
     * Example: "4000000000001091" -> "4000 0000 0000 1091"
     *
     * @param newCardNumber - card number to format
     * @param previousCardNumber - previous value that was entered into the card field. This value
     * is needed to handle the deletion of characters.
     */
    fun formatCardNumber(newCardNumber: String, previousCardNumber: String = ""): String {
        if (newCardNumber.length < previousCardNumber.length) return newCardNumber
        var cardString = newCardNumber.replace(" ", "")

        val cardType = getCardType(cardString)
        return if (previousCardNumber.length == cardType.maxCardLength + cardType.cardNumberIndices.size) {
            previousCardNumber
        } else {
            for (index in cardType.cardNumberIndices) {
                if (index <= cardString.length) {
                    cardString = cardString.insertChar(index, ' ')
                }
            }
            cardString
        }
    }

    private fun String.insertChar(index: Int, char: Char): String {
        return substring(0, index) + char + substring(index, length)
    }
}
