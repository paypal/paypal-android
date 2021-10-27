package com.paypal.android.checkout.pojo

import com.paypal.android.checkout.CurrencyCode
import com.paypal.android.checkout.asNativeCheckout
import com.paypal.android.checkout.asPaypalCheckout
import com.paypal.checkout.order.Items
import com.paypal.checkout.order.Order

/**
 * This is the data returned when the [OnShippingChange.onShippingChanged] callback is invoked.
 */
data class ShippingChangeData(
    /**
     * The token for the transaction
     */
    val payToken: String,

    /**
     * Payment ID for the transaction
     */
    val paymentId: String?,

    /**
     * Shipping change event type. This enum will indicate whether the buyer has updated their
     * shipping address or selected a different shipping or pick up option.
     * See [ShippingChangeType].
     */
    val shippingChangeType: ShippingChangeType,

    /**
     * The shipping address the buyer has entered
     */
    val shippingAddress: ShippingAddress,

    /**
     * List of all shipping and pick up options. Note that [Options.selected] will indicate which
     * option the buyer has selected.
     */
    val shippingOptions: List<Options>
) {
    internal constructor(shippingChangeData: com.paypal.checkout.shipping.ShippingChangeData) : this(
        payToken = shippingChangeData.payToken,
        paymentId = shippingChangeData.paymentId,
        shippingChangeType = shippingChangeData.shippingChangeType.asPaypalCheckout,
        shippingAddress = ShippingAddress(shippingChangeData.shippingAddress),
        shippingOptions = shippingChangeData.shippingOptions.map { options -> Options(options) }
            .toList()
    )

    /**
     * Shipping option the buyer has selected
     */
    val selectedShippingOption: Options?
        get() = shippingOptions.find { it.selected }
}

data class ShippingAddress(
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val adminArea1: String? = null,
    val adminArea2: String? = null,
    val postalCode: String? = null,
    val countryCode: String
) {
    internal constructor(address: com.paypal.checkout.order.Address) : this(
        addressLine1 = address.addressLine1,
        addressLine2 = address.addressLine2,
        adminArea1 = address.adminArea1,
        adminArea2 = address.adminArea2,
        postalCode = address.postalCode,
        countryCode = address.countryCode
    )

    internal val asNativeCheckout: com.paypal.checkout.order.Address
        get() = com.paypal.checkout.order.Address(
            addressLine1 = this.addressLine1,
            addressLine2 = this.addressLine2,
            adminArea1 = this.adminArea1,
            adminArea2 = this.adminArea2,
            postalCode = this.postalCode,
            countryCode = this.countryCode
        )
}

data class Options(
    val id: String,
    val selected: Boolean,
    val label: String,
    val type: ShippingType? = null,
    val amount: UnitAmount? = null
) {
    internal constructor(options: com.paypal.checkout.order.Options) : this(
        id = options.id,
        selected = options.selected,
        label = options.label,
        type = options.type?.asPaypalCheckout,
        amount = if (options.amount != null) UnitAmount(options.amount!!) else null
    )

    internal val toNativeCheckout: com.paypal.checkout.order.Options
        get() = com.paypal.checkout.order.Options(
            this.id,
            this.selected,
            this.label,
            this.type?.asNativeCheckout,
            this.amount?.toNativeCheckout
        )
}

data class UnitAmount(
    val currencyCode: CurrencyCode,
    val value: String
) {
    internal constructor(unitAmount: com.paypal.checkout.order.UnitAmount) : this(
        currencyCode = unitAmount.currencyCode.asPaypalCheckout,
        value = unitAmount.value
    )

    internal val toNativeCheckout: com.paypal.checkout.order.UnitAmount
        get() = com.paypal.checkout.order.UnitAmount(this.currencyCode.asNativeCheckout, this.value)
}

/*
* The method by which the payer wants to get their items.
*/
enum class ShippingType {
    /**
     * The payer intends to receive the items at a specified address.
     */
    SHIPPING,

    /**
     * The payer intends to pick up the items at a specified address. For example, a store address.
     */
    PICKUP
}

internal val ShippingType.asNativeCheckout: com.paypal.checkout.createorder.ShippingType
    get() = enumValueOf(this.name)

internal val com.paypal.checkout.createorder.ShippingType.asPaypalCheckout: ShippingType
    get() = enumValueOf(this.name)

/**
 * Indicates the type of shipping change
 */
enum class ShippingChangeType {

    /**
     * The buyer has updated their shipping address.
     */
    ADDRESS_CHANGE,

    /**
     * The buyer has selected a different shipping or pickup option
     */
    OPTION_CHANGE
}

internal val ShippingChangeType.asNativeCheckout: com.paypal.checkout.shipping.ShippingChangeType
    get() = enumValueOf(this.name)

internal val com.paypal.checkout.shipping.ShippingChangeType.asPaypalCheckout: ShippingChangeType
    get() = enumValueOf(this.name)

/**
 * The total order amount with an optional breakdown that provides details, such as the total
 * item amount, total tax amount, shipping, handling, and discounts, if any.
 *
 * If you specify [breakdown], the amount equals [BreakDown.itemTotal] plus [BreakDown.taxTotal]
 * plus [BreakDown.shipping] plus [BreakDown.handling] minus [BreakDown.shippingDiscount] minus
 * [BreakDown.discount].
 *
 * The amount must be a positive number.
 */
data class OrderAmount constructor(

    /**
     * The three-character ISO-4217 currency code that identifies the currency. See [CurrencyCode].
     */
    val currencyCode: CurrencyCode,

    /**
     * The value, which might be:
     *    - An integer for currencies like JPY that are not typically fractional.
     *    - A decimal fraction for currencies like TND that are subdivided into thousandths.
     *
     * For the required number of decimal places for a currency code, see Currency Codes.
     */
    val value: String,

    /**
     * The breakdown of the amount. Breakdown provides details such as total item amount, total tax
     * amount, shipping, handling, insurance, and discounts, if any.
     */
    val breakdown: BreakDown? = null
) {
    internal val asNativeCheckout: com.paypal.checkout.order.Amount
        get() = com.paypal.checkout.order.Amount(
            currencyCode = currencyCode.asNativeCheckout,
            value = value,
            breakdown = breakdown?.asNativeCheckout
        )
}

/**
 * The breakdown of the amount. Breakdown provides details such as total item amount, total tax
 * amount, shipping, handling, insurance, and discounts, if any.
 */
data class BreakDown(

    /**
     * The subtotal for all items. Required if the [Order] includes [Items.unitAmount]. Must equal
     * the sum of ([Items.unitAmount] * [Items.quantity]) for all items. [itemTotal]'s value can not
     * be a negative number.
     */
    val itemTotal: UnitAmount? = null,

    /**
     * The shipping fee for all items within a given purchase unit. [shipping]' value can not be a
     * negative number.
     */
    val shipping: UnitAmount? = null,

    /**
     * The handling fee for all items within a given purchase unit. [handling]'s value can not be a
     * negative number.
     */
    val handling: UnitAmount? = null,

    /**
     * The total tax for all items. Required if the order includes [Items.tax]. Must equal the sum
     * of ([Items.tax] * [Items.quantity]) for all items. [taxTotal]'s value can not be a negative
     * number.
     */
    val taxTotal: UnitAmount? = null,

    /**
     * The shipping discount for all items within a given purchase unit. [shippingDiscount]'s value
     * can not be a negative number.
     */
    val shippingDiscount: UnitAmount? = null,

    /**
     * The discount for all items within a given purchase unit. [discount]'s value can not be a
     * negative number.
     */
    val discount: UnitAmount? = null
) {
    internal val asNativeCheckout: com.paypal.checkout.order.BreakDown
        get() = com.paypal.checkout.order.BreakDown(
            itemTotal = itemTotal?.toNativeCheckout,
            shipping = shipping?.toNativeCheckout,
            handling = handling?.toNativeCheckout,
            taxTotal = taxTotal?.toNativeCheckout,
            shippingDiscount = shippingDiscount?.toNativeCheckout,
            discount = discount?.toNativeCheckout
        )
}
