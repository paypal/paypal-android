package com.paypal.android.paypalnativepayments

import com.paypal.checkout.order.Address
// TODO: this object is diferent in iOS, check with MXO for better consistency
/**
 * The user's selected shipping address via the PayPal Native Checkout UI.
 */
data class PayPalNativeShippingAddress internal constructor(
    /**
     * Line 1 of the Address (eg. number, street, etc)
     */
    val addressLine1: String?,

    /**
     * Line 2 of the Address (eg. suite, apt #, etc.)
     */
    val addressLine2: String?,
    /**
     * The highest level sub-division in a country, which is usually a province, state, or ISO-3166-2 subdivision.
     * Format for postal delivery. For example, `CA` and not `California`. Value, by country, is:
     * - UK: A county.
     * - US: A state.
     * - Canada: A province.
     * - Japan: A prefecture.
     * - Switzerland: A kanton.
     */
    val adminArea1: String?,

    /**
     * The city, town, or village. Smaller than `adminArea1`
     */
    val adminArea2: String?,
    /**
     * The postal code, which is the zip code or equivalent. Typically required for countries with
     * a postal code or an equivalent.
     */
    val postalCode: String?,
    /**
     * The two-character ISO 3166-1 code that identifies the country or region.
     * For more information, refer to: https://developer.paypal.com/api/rest/reference/country-codes/
     */
    val countryCode: String?
) {

    internal constructor(address: Address) : this(
        address.addressLine1,
        address.addressLine2,
        address.adminArea1,
        address.adminArea2,
        address.postalCode,
        address.countryCode
    )
}
