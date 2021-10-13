package com.paypal.android.checkout.patch

import com.paypal.android.checkout.pojo.ShippingAddress


/**
 * Class containing the different operations that can be performed with an [Address].
 */
sealed class PatchShippingAddress(
    purchaseUnitReferenceId: String,
    patchOperation: PatchOperation,
    val address: ShippingAddress
) : OrderUpdate(purchaseUnitReferenceId, patchOperation, address) {

    /**
     * Adds a new shipping [Address] to a [com.paypal.checkout.order.PurchaseUnit].
     *
     * @param address - [Address] to add
     * @param purchaseUnitReferenceId - reference ID of the purchase unit. This param is only needed
     * when there are multiple purchase units on the order.
     * See [com.paypal.checkout.order.PurchaseUnit.referenceId].
     */
    class Add(
        address: ShippingAddress,
        purchaseUnitReferenceId: String = DEFAULT_PURCHASE_UNIT_ID
    ) : PatchShippingAddress(purchaseUnitReferenceId, PatchOperation.ADD, address) {

        override fun toNativeCheckout(): com.paypal.checkout.order.patch.OrderUpdate =
            com.paypal.checkout.order.patch.fields.PatchShippingAddress.Add(
                address.asNativeCheckout,
                purchaseUnitReferenceId
            )
    }

    /**
     * Replaces a shipping [Address] on a [com.paypal.checkout.order.PurchaseUnit].
     *
     * @param address - [Address] to replace
     * @param purchaseUnitReferenceId - reference ID of the purchase unit. This param is only needed
     * when there are multiple purchase units on the order.
     * See [com.paypal.checkout.order.PurchaseUnit.referenceId].
     */
    class Replace(
        address: ShippingAddress,
        purchaseUnitReferenceId: String = DEFAULT_PURCHASE_UNIT_ID
    ) : PatchShippingAddress(purchaseUnitReferenceId, PatchOperation.REPLACE, address) {

        override fun toNativeCheckout(): com.paypal.checkout.order.patch.OrderUpdate =
            com.paypal.checkout.order.patch.fields.PatchShippingAddress.Replace(
                address.asNativeCheckout,
                purchaseUnitReferenceId
            )

    }

    override fun getPath() =
        "/purchase_units/@reference_id=='$purchaseUnitReferenceId'/shipping/address"
}