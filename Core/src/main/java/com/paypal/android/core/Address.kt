package com.paypal.android.core

data class Address(

    /**
     * Optional. Line 1 of the Address (eg. number, street, etc)
     */
    val streetAddress: String? = null,

    /**
     * Optional. Line 2 of the Address (eg. suite, apt #, etc.)
     */
    val extendedAddress: String? = null,

    /**
     * Optional. City name
     */
    val locality: String? = null,

    /**
     * Optional. Either a two-letter state code (for the US), or an
     * ISO-3166-2 country subdivision code of up to three letters.
     */
    val region: String? = null,

    /**
     * Optional. Zip code or equivalent is usually required for countries that have them.
     * For a list of countries that do not have postal codes please refer to http://en.wikipedia.org/wiki/Postal_code
     */
    val postalCode: String? = null,

    /**
     * Optional. 2 letter country code
     */
    val countryCode: String? = null,
)
