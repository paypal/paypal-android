package com.paypal.android.checkout.pojo

import com.paypal.android.checkout.CurrencyCode
import com.paypal.android.checkout.asPaypalCheckout


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
        get() = shippingOptions.find { it.selected ?: false }
}


data class ShippingAddress(
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val adminArea1: String? = null,
    val adminArea2: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null
) {
    internal constructor(address: com.paypal.checkout.order.Address?) : this(
        addressLine1 = address?.addressLine1,
        addressLine2 = address?.addressLine2,
        adminArea1 = address?.adminArea1,
        adminArea2 = address?.adminArea2,
        postalCode = address?.postalCode,
        countryCode = address?.countryCode
    )
}


data class Options(
    val id: String? = null,
    val selected: Boolean? = null,
    val label: String? = null,
    val type: ShippingType? = null,
    val amount: UnitAmount? = null
) {
    internal constructor(options: com.paypal.checkout.order.Options?) : this(
        id = options?.id,
        selected = options?.selected,
        label = options?.label,
        type = options?.type?.asPaypalCheckout,
        amount = UnitAmount(options?.amount)
    )
}


data class UnitAmount(
    val currencyCode: CurrencyCode? = null,
    val value: String? = null
) {
    internal constructor(unitAmount: com.paypal.checkout.order.UnitAmount?) : this(
        currencyCode = unitAmount?.currencyCode?.asPaypalCheckout,
        value = unitAmount?.value
    )
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
    get() = when (this) {
        ShippingType.SHIPPING -> com.paypal.checkout.createorder.ShippingType.SHIPPING
        ShippingType.PICKUP -> com.paypal.checkout.createorder.ShippingType.PICKUP
    }

internal val com.paypal.checkout.createorder.ShippingType.asPaypalCheckout: ShippingType
    get() = when (this) {
        com.paypal.checkout.createorder.ShippingType.SHIPPING -> ShippingType.SHIPPING
        com.paypal.checkout.createorder.ShippingType.PICKUP -> ShippingType.PICKUP
    }


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
    get() = when (this) {
        ShippingChangeType.ADDRESS_CHANGE -> com.paypal.checkout.shipping.ShippingChangeType.ADDRESS_CHANGE
        ShippingChangeType.OPTION_CHANGE -> com.paypal.checkout.shipping.ShippingChangeType.OPTION_CHANGE
    }

internal val com.paypal.checkout.shipping.ShippingChangeType.asPaypalCheckout: ShippingChangeType
    get() = when (this) {
        com.paypal.checkout.shipping.ShippingChangeType.ADDRESS_CHANGE -> ShippingChangeType.ADDRESS_CHANGE
        com.paypal.checkout.shipping.ShippingChangeType.OPTION_CHANGE -> ShippingChangeType.OPTION_CHANGE
    }