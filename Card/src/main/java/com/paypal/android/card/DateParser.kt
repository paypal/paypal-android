package com.paypal.android.card

internal class DateParser {

    fun parseCardExpiry(input: String): CardExpiry {
        val parsedSecurityCode = input.split("/")
        val month = "20${parsedSecurityCode[0]}"
        val year = parsedSecurityCode[1]
        return CardExpiry(month, year)
    }
}
