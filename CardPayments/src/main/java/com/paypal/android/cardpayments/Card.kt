package com.paypal.android.cardpayments

import android.os.Parcelable
import com.paypal.android.corepayments.Address
import kotlinx.parcelize.Parcelize

/**
 * Represents raw credit or debit card data provided by the customer.
 */
@Parcelize
data class Card @JvmOverloads constructor(

    /**
     * The primary account number (PAN) for the payment card
     */
    var number: String,

    /**
     * The 2-digit card expiration month in `MM` format
     */
    val expirationMonth: String,

    /**
     * The 4-digit card expiration year in `YYYY` format
     */
    val expirationYear: String,

    /**
     * The three- or four-digit security code of the card. Also known as the CVV, CVC, CVN, CVE, or CID.
     */
    var securityCode: String,

    /**
     * Optional. The card holder's name as it appears on the card
     */
    var cardholderName: String? = null,

    /**
     * Optional. The billing address
     */
    var billingAddress: Address? = null,
) : Parcelable
