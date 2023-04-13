package com.paypal.android.paypalnativepayments

import com.paypal.checkout.order.Options

/**
 * Shipping method details for an order via the PayPal Native Checkout UI.
 * If you want to show shipping options in the PayPal Native Paysheet,
 * provide `purchase_units[].shipping.options` when creating an orderID with
 * the [`orders/v2` API](https://developer.paypal.com/docs/api/orders/v2/#definition-purchase_unit)
 * on your server. Otherwise, our Paysheet won't display any shipping options.
 */
data class PayPalNativeShippingMethod internal constructor(

    /**
     * A unique ID that identifies a payer-selected shipping option.
     */
    val id: String,

    /**
     * If true it represents the shipping option that the merchant expects to be selected for the buyer
     * when they view the shipping options within the PayPal Checkout experience.
     * The selected shipping option must match the shipping cost in the order breakdown.
     * Only one shipping option per purchase unit can be selected.
     */
    val selected: Boolean,

    /**
     * A description that the payer sees, which helps them choose an appropriate shipping option.
     * For example, Free Shipping, USPS Priority Shipping, Expédition prioritaire USPS, or USPS yōuxiān fā huò.
     * Localize this description to the payer's locale.
     */
    val label: String,

    /**
     * The method by which the payer wants to get their items.
     */
    val type: PayPalNativeShippingType?,

    /**
     * The shipping cost for the selected option, which might be:
     * An integer for currencies like JPY that are not typically fractional.
     * A decimal fraction for currencies like TND that are subdivided into thousandths.
     * Maximum length: 32.
     * Pattern: ^((-?[0-9]+)|(-?([0-9]+)?[.][0-9]+))$
     */
    val value: String?,

    /**
     * The [three-character ISO-4217 currency code](https://developer.paypal.com/docs/api/reference/currency-codes/)
     * that identifies the currency.
     * Currency code in text format (example: "USD")
     */
    val currencyCode: String?
) {

    internal constructor(options: Options) : this(
        options.id,
        options.selected,
        options.label,
        PayPalNativeShippingType.fromShippingType(options.type),
        options.amount?.value,
        options.amount?.currencyCode?.name
    )
}
