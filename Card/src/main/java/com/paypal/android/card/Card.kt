package com.paypal.android.card

import com.paypal.android.corepayments.Address

/**
 * Initialize a card object
 */

data class Card @JvmOverloads constructor(

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
     * Optional. The card's security code (CVV, CVC, CVN, CVE, or CID)
     */
    var securityCode: String? = null,

    /**
     * Optional. The card holder's name as it appears on the card
     */
    var cardholderName: String? = null,

    /**
     * Optional. The portable international postal address
     */
    var billingAddress: Address? = null,
)
