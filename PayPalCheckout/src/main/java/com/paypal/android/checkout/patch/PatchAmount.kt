package com.paypal.android.checkout.patch

import com.paypal.android.checkout.pojo.Amount
import com.paypal.android.checkout.pojo.OrderAmount


/**
 * Class containing the different operations that can be performed with [Amount].
 */
sealed class PatchAmount(
    purchaseUnitReferenceId: String,
    patchOperation: PatchOperation,
    val amount: OrderAmount //TODO: change pojo amount
) : OrderUpdate(purchaseUnitReferenceId, patchOperation, amount) {

    /**
     * Replaces the [Amount] on a [com.paypal.checkout.order.PurchaseUnit].
     *
     * @param amount - [Amount] to replace
     * @param purchaseUnitReferenceId - reference ID of the purchase unit. This param is only needed
     * when there are multiple purchase units on the order.
     * See [com.paypal.checkout.order.PurchaseUnit.referenceId].
     */
    class Replace(
        amount: OrderAmount,
        purchaseUnitReferenceId: String = DEFAULT_PURCHASE_UNIT_ID
    ) : PatchAmount(purchaseUnitReferenceId, PatchOperation.REPLACE, amount) {
        override fun toNativeCheckout(): com.paypal.checkout.order.patch.OrderUpdate =
            com.paypal.checkout.order.patch.fields.PatchAmount.Replace(
                amount.asNativeCheckout,
                purchaseUnitReferenceId
            )

    }

    override fun getPath() = "/purchase_units/@reference_id=='$purchaseUnitReferenceId'/amount"
}