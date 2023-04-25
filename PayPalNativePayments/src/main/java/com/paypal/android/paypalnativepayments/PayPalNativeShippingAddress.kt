package com.paypal.android.paypalnativepayments

import com.paypal.checkout.shipping.ShippingChangeAddress

/**
 * The user's selected shipping address via the PayPal Native Checkout UI.
 */
data class PayPalNativeShippingAddress internal constructor(
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

    internal constructor(shippingChangeAddress: ShippingChangeAddress) : this(
        shippingChangeAddress.adminArea1,
        shippingChangeAddress.adminArea2,
        shippingChangeAddress.postalCode,
        shippingChangeAddress.countryCode
    )
}
