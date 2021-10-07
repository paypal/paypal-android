package com.paypal.android.card

import com.paypal.android.core.Address

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
    var securityCode: String? = null,

    /**
     * The card holder's name as it appears on the card
     */
    var name: String? = null,

    /**
     * The portable international postal address
     */
    var billingAddress: Address? = null,
)
