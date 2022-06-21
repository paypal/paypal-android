package com.paypal.android.ui.card.validation

object DateFormatter {

    /**
     * Formats a month and year string for UI.
     * Example: "1226" -> "12/26"
     *
     * @param newDateString - date to format
     * @param previousDateString - previous value that was entered into the date field. This value
     * is needed to handle the deletion of characters.
     */
    @Suppress("MagicNumber")
    fun formatExpirationDate(
        newDateString: String,
        previousDateString: String?
    ): String {
        if (newDateString.endsWith("/")) return newDateString
        return if (newDateString.length > (previousDateString ?: "").length) {
            when (newDateString.length) {
                2 -> "$newDateString/"
                3 -> newDateString.substring(0, 2) + "/" + newDateString.substring(2, 3)
                else -> newDateString
            }
        } else newDateString
    }
}
