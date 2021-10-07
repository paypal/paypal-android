package com.paypal.android.checkout.pojo


import java.util.HashMap


data class Amounts(
    val handlingFee: HandlingFee? = null,
    val insurance: Insurance? = null,
    val shippingAndHandling: ShippingAndHandling? = null,
    val shippingDiscount: ShippingDiscount? = null,
    val subtotal: Subtotal? = null,
    val tax: Tax? = null,
    val total: Total? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(amounts: com.paypal.pyplcheckout.pojo.Amounts?) : this(
        handlingFee = HandlingFee(amounts?.handlingFee),
        insurance = Insurance(amounts?.insurance),
        shippingAndHandling = ShippingAndHandling(amounts?.shippingAndHandling),
        shippingDiscount = ShippingDiscount(amounts?.shippingDiscount),
        subtotal = Subtotal(amounts?.subtotal),
        tax = Tax(amounts?.tax),
        total = Total(amounts?.total),
        additionalProperties = amounts?.additionalProperties
    )
}

class HandlingFee(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: MutableMap<String, Any>? = HashMap()
) {
    internal constructor(handlingFee: com.paypal.pyplcheckout.pojo.HandlingFee?) : this(
        currencyFormatSymbolISOCurrency = handlingFee?.currencyFormatSymbolISOCurrency,
        additionalProperties = handlingFee?.additionalProperties
    )
}


data class Insurance(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(insurance: com.paypal.pyplcheckout.pojo.Insurance?) : this(
        currencyFormatSymbolISOCurrency = insurance?.currencyFormatSymbolISOCurrency,
        additionalProperties = insurance?.additionalProperties
    )
}


data class ShippingAndHandling(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(shippingAndHandling: com.paypal.pyplcheckout.pojo.ShippingAndHandling?) : this(
        currencyFormatSymbolISOCurrency = shippingAndHandling?.currencyFormatSymbolISOCurrency,
        additionalProperties = shippingAndHandling?.additionalProperties
    )
}


data class ShippingDiscount(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor (shippingDiscount: com.paypal.pyplcheckout.pojo.ShippingDiscount?) : this(
        currencyFormatSymbolISOCurrency = shippingDiscount?.currencyFormatSymbolISOCurrency,
        additionalProperties = shippingDiscount?.additionalProperties
    )
}


data class Subtotal(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(subtotal: com.paypal.pyplcheckout.pojo.Subtotal?) : this(
        currencyFormatSymbolISOCurrency = subtotal?.currencyFormatSymbolISOCurrency,
        additionalProperties = subtotal?.additionalProperties
    )
}


data class Tax(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(tax: com.paypal.pyplcheckout.pojo.Tax?) : this(
        currencyFormatSymbolISOCurrency = tax?.currencyFormatSymbolISOCurrency,
        additionalProperties = tax?.additionalProperties
    )
}


data class UnitPrice(
    val currencyFormatSymbolISOCurrency: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(unitPrice: com.paypal.pyplcheckout.pojo.UnitPrice?) : this(
        currencyFormatSymbolISOCurrency = unitPrice?.currencyFormatSymbolISOCurrency,
        additionalProperties = unitPrice?.additionalProperties
    )
}


data class Total(
    val currencyCode: String? = null,
    val currencyFormat: String? = null,
    val currencyFormatSymbolISOCurrency: String? = null,
    val currencyValue: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap(),
) {
    internal constructor(total: com.paypal.pyplcheckout.pojo.Total?) : this(
        currencyCode = total?.currencyCode,
        currencyFormat = total?.currencyFormat,
        currencyFormatSymbolISOCurrency = total?.currencyFormatSymbolISOCurrency,
        currencyValue = total?.currencyValue,
        additionalProperties = total?.additionalProperties
    )
}


class Amount(
    val currencyFormat: String? = null,
    val currencyFormatSymbolISOCurrency: String? = null,
    val currencyCode: String? = null,
    val currencySymbol: String? = null,
    val currencyValue: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(amount: com.paypal.pyplcheckout.pojo.Amount?) : this(
        currencyCode = amount?.currencyCode,
        currencyFormat = amount?.currencyFormat,
        currencyFormatSymbolISOCurrency = amount?.currencyFormatSymbolISOCurrency,
        currencyValue = amount?.currencyValue,
        currencySymbol = amount?.currencySymbol,
        additionalProperties = amount?.getAdditionalProperties()
    )
}
