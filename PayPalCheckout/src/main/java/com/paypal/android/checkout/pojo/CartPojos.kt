package com.paypal.android.checkout.pojo

import java.util.HashMap


data class Cart(
    val cartId: String? = null,
    val intent: String? = null,
    val paymentId: String? = null,
    val billingToken: String? = null,
    val items: List<Item>? = null,
    val amounts: Amounts? = null,
    val cancelUrl: CancelUrl? = null,
    val returnUrl: ReturnUrl? = null,
    val total: Total? = null,
    val shippingMethods: List<ShippingMethods>? = null,
    val shippingAddress: CartShippingAddress? = null
) {
    internal constructor(cart: com.paypal.pyplcheckout.pojo.Cart?) : this(
        cartId = cart?.cartId,
        intent = cart?.intent,
        paymentId = cart?.paymentId,
        billingToken = cart?.billingToken,
        items = cart?.items?.map { item -> Item(item) }?.toList(),
        amounts = Amounts(cart?.amounts),
        cancelUrl = CancelUrl(cart?.cancelUrl),
        returnUrl = ReturnUrl(cart?.returnUrl),
        total = Total(cart?.total),
        shippingMethods = cart?.shippingMethods?.map { method -> ShippingMethods(method) }
            ?.toList(),
        shippingAddress = CartShippingAddress(cart?.shippingAddress)
    )
}


data class Item(
    val description: String? = null,
    val name: String? = null,
    val unitPrice: UnitPrice? = null,
    val quantity: Int? = null,
    val total: Total? = null,
    val details: Any? = null,
    val additionalProperties: Map<String, Any>? = HashMap(),
) {
    internal constructor(item: com.paypal.pyplcheckout.pojo.Item?) : this(
        description = item?.description,
        name = item?.name,
        unitPrice = UnitPrice(item?.unitPrice),
        quantity = item?.quantity,
        total = Total(item?.total),
        details = item?.details,
        additionalProperties = item?.additionalProperties
    )
}


data class CancelUrl(
    val href: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(cancelUrl: com.paypal.pyplcheckout.pojo.CancelUrl?) : this(
        href = cancelUrl?.href,
        additionalProperties = cancelUrl?.additionalProperties
    )
}


data class ReturnUrl(
    val href: String? = null,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(returnUrl: com.paypal.pyplcheckout.pojo.ReturnUrl?) : this(
        href = returnUrl?.href,
        additionalProperties = returnUrl?.additionalProperties
    )
}


data class CartShippingAddress(
    val firstName: String? = null,
    val lastName: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val isFullAddress: Boolean? = null,
    val isStoreAddress: Boolean? = null
) {
    internal constructor(cartShippingAddress: com.paypal.pyplcheckout.pojo.CartShippingAddress?) : this(
        firstName = cartShippingAddress?.firstName,
        lastName = cartShippingAddress?.lastName,
        line1 = cartShippingAddress?.line1,
        line2 = cartShippingAddress?.line2,
        city = cartShippingAddress?.city,
        state = cartShippingAddress?.state,
        postalCode = cartShippingAddress?.postalCode,
        country = cartShippingAddress?.country,
        isFullAddress = cartShippingAddress?.isFullAddress,
        isStoreAddress = cartShippingAddress?.isStoreAddress
    )
}


data class ShippingMethods(
    val id: String,
    val label: String,
    var selected: Boolean,
    val amount: Amount?,
    val type: String
) {
    internal constructor(shippingMethods: com.paypal.pyplcheckout.pojo.ShippingMethods) : this(
        id = shippingMethods.id,
        label = shippingMethods.label,
        selected = shippingMethods.selected,
        amount = if (shippingMethods.amount != null) Amount(shippingMethods.amount!!) else null,
        type = shippingMethods.type
    )
}
