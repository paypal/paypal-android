package com.paypal.android.card

/**
 * Initialize a card object
 */
data class Card(

    /**
     * The card number
     */
    var number: String,

    /**
     * 2-digit card expiration month
     */
    val expirationMonth: String,

    /**
     * 4-digit card expiration year
     */
    val expirationYear: String,

    /**
     * The card's security code (CVV, CVC, CVN, CVE, or CID)
     */
    var securityCode: String = ""
)
