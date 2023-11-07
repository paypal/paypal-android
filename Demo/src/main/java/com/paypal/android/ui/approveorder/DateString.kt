package com.paypal.android.ui.approveorder

import java.lang.Integer.min

data class DateString(private val rawDate: String) {

    private val month: String
    private val year: String

    private val didPadZero: Boolean
    private val didAddSlash: Boolean

    val formatted: String
    val formattedMonth: String
    val formattedYear: String

    companion object {
        private const val maxMonthLength = 2
        private val leadingMonthCharacter = setOf('0', '1')
        fun startsWithLeadingMonthCharacter(str: String) =
            if (str.isNotEmpty()) leadingMonthCharacter.contains(str[0]) else false
    }

    init {

        var padZero = false
        var addSlash = false

        if (rawDate.isEmpty()) {
            month = ""
            year = ""
        } else {
            month = if (startsWithLeadingMonthCharacter(rawDate)) {
                // assume month is first two characters; clamp to length of string
                // to prevent out of bounds access
                rawDate.substring(0, min(rawDate.length, maxMonthLength))
            } else {
                // assume month is a single digit; keep track of modification
                padZero = true
                "${rawDate[0]}"
            }
            // year is remainder
            year = rawDate.substring(month.length)
        }

        var formatted = ""
        if (padZero) {
            // pad month with zero
            formatted += "0"
        }
        formatted += month

        if (padZero || year.isNotEmpty()) {
            // add slash between month and year
            formatted += "/"
            addSlash = true
        }
        formatted += year

        this.didPadZero = padZero
        this.didAddSlash = addSlash
        this.formatted = formatted

        // TODO: handle invalid date string
        var formattedMonth = ""
        var formattedYear = ""

        val dateStringComponents = formatted.split("/")
        if (dateStringComponents.isNotEmpty()) {
            formattedMonth = dateStringComponents[0]
            if (dateStringComponents.size > 1) {
                val rawYear = dateStringComponents[1]

                // assume date in 2000's and pad with "20" (if necessary)
                formattedYear = if (rawYear.length == 2) "20$rawYear" else rawYear
            }
        }

        this.formattedMonth = formattedMonth
        this.formattedYear = formattedYear
    }

    fun mapRawOffsetToFormatted(rawOffset: Int): Int {
        var adjustment = 0
        if (rawOffset >= month.length) {
            if (didPadZero) {
                adjustment += 1
            }

            if (didAddSlash) {
                adjustment += 1
            }
        }
        return rawOffset + adjustment
    }

    fun mapFormattedOffsetToRawOffset(formattedOffset: Int): Int {
        var adjustment = 0
        if (formattedOffset >= "XX/".length) {
            if (didAddSlash) {
                adjustment -= 1
            }

            if (didPadZero) {
                adjustment -= 1
            }
        }
        return formattedOffset + adjustment
    }
}
