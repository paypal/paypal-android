package com.paypal.android.card

internal class DateParser {

    fun parseCardExpiry(input: String): CardExpiry {
        val parsedSecurityCode = input.split("/")
        val month = parsedSecurityCode[0]
        val year = "20${parsedSecurityCode[1]}"
        return CardExpiry(month, year)
    }
}
