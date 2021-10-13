package com.paypal.android.checkout.shipping

import com.paypal.android.checkout.patch.PatchOrderRequest

/**
 * The actions that can be performed when the [OnShippingChange.onShippingChanged] callback is
 * invoked.
 */
class ShippingChangeActions internal constructor(private val mChangeActions: com.paypal.checkout.shipping.ShippingChangeActions) {

    /**
     * Call patchOrder() to update an order. The patch action will only work for orders with the
     * CREATED or APPROVED status. You cannot update an order with the COMPLETED status.
     *
     * If an error occurs in the patch order action, the [com.paypal.checkout.error.OnError.onError]
     * callback will be invoked.
     *
     * @param patchOrderRequest - Request containing the order updates
     * @param onComplete - callback for when the order was successfully updated
     */
    fun patchOrder(patchOrderRequest: PatchOrderRequest, onComplete: () -> Unit) {
        mChangeActions.patchOrder(patchOrderRequest.toNativeCheckout, onComplete)
    }

    /**
     * Call patchOrder() to update an order. The patch action will only work for orders with the
     * CREATED or APPROVED status. You cannot update an order with the COMPLETED status.
     *
     * If an error occurs in the patch order action, the [com.paypal.checkout.error.OnError.onError]
     * callback will be invoked.
     *
     * @param patchOrderRequest - Request containing the order updates
     * @param onComplete - callback for when the order was successfully updated
     */
    fun patchOrder(patchOrderRequest: PatchOrderRequest, onComplete: OnPatchComplete) {
        mChangeActions.patchOrder(patchOrderRequest.toNativeCheckout, object :
            com.paypal.checkout.order.actions.OnPatchComplete {
            override fun onPatchComplete() {
                onComplete.onPatchComplete()
            }
        })
    }

    /**
     * Call [reject] when a buyer selects a shipping option that is not supported or has entered a
     * shipping address that is not supported. The paysheet will require the buyer to fix the issue
     * before continuing with the order.
     */
    fun reject() {
        mChangeActions.reject()
    }

}

/**
 * This interface definition is to provide a callback when a patch order request has completed.
 */
interface OnPatchComplete {

    /**
     * Called when a patch order request has completed. This will only be called when a patch
     * request has succeeded. If an error occurs, the [com.paypal.checkout.error.OnError.onError]
     * callback will be invoked.
     */
    fun onPatchComplete()

    companion object {
        operator fun invoke(patchComplete: () -> Unit): OnPatchComplete {
            return object : OnPatchComplete {
                override fun onPatchComplete() {
                    patchComplete()
                }
            }
        }
    }
}