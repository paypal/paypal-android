package com.paypal.android.card

internal class DateParser {

    fun parseExpirationDate(input: String): ExpirationDate {
        val parsedSecurityCode = input.split("/")
        val (monthString, yearString) = parsedSecurityCode
        val month = monthString.toInt()
        val year = "20$yearString".toInt()
        return ExpirationDate(month, year)
    }
}
