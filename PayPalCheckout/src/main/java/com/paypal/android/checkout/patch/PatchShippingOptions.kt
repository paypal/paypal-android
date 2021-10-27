package com.paypal.android.checkout.patch

import com.paypal.android.checkout.pojo.Options

/**
 * Class containing the different operations that can be performed with [Options].
 */
sealed class PatchShippingOptions(
    purchaseUnitReferenceId: String,
    patchOperation: PatchOperation,
    val options: List<Options>
) : OrderUpdate(purchaseUnitReferenceId, patchOperation, options) {

    /**
     * Replaces the shipping [Options] on a [com.paypal.checkout.order.PurchaseUnit].
     *
     * @param options - list of [Options] to replace
     * @param purchaseUnitReferenceId - reference ID of the purchase unit. This param is only needed
     * when there are multiple purchase units on the order.
     * See [com.paypal.checkout.order.PurchaseUnit.referenceId].
     */
    class Replace(
        options: List<Options>,
        purchaseUnitReferenceId: String = DEFAULT_PURCHASE_UNIT_ID
    ) : PatchShippingOptions(purchaseUnitReferenceId, PatchOperation.REPLACE, options) {

        override fun toNativeCheckout(): com.paypal.checkout.order.patch.OrderUpdate {
            return com.paypal.checkout.order.patch.fields.PatchShippingOptions.Replace(
                options = options.map { option -> option.toNativeCheckout },
                purchaseUnitReferenceId = purchaseUnitReferenceId
            )
        }
    }

    override fun getPath() =
        "/purchase_units/@reference_id=='$purchaseUnitReferenceId'/shipping/options"
}
