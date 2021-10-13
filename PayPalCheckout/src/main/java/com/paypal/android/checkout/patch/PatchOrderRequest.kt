package com.paypal.android.checkout.patch

/**
 * Request containing the order updates for patching an order.
 *
 * Use the constructor containing the vararg to create a new [PatchOrderRequest].
 */
data class PatchOrderRequest internal constructor(
    val orderUpdates: List<OrderUpdate>
) {
    /**
     * Creates a new [PatchOrderRequest].
     *
     * @param orderUpdate - updates to be performed on the order
     */
    constructor(vararg orderUpdate: OrderUpdate) : this(orderUpdate.asList())

    internal val toNativeCheckout: com.paypal.checkout.order.patch.PatchOrderRequest
        get() = this.transform()

    private fun transform() : com.paypal.checkout.order.patch.PatchOrderRequest {
        val orderUpdateNativeList = orderUpdates.map { orderUpdate -> orderUpdate.toNativeCheckout()  }
        return com.paypal.checkout.order.patch.PatchOrderRequest(*orderUpdateNativeList.toTypedArray())
    }
}

abstract class OrderUpdate(
    val purchaseUnitReferenceId: String,
    val patchOperation: PatchOperation,
    val value: Any
) {
    internal abstract fun getPath(): String

    companion object {
        const val DEFAULT_PURCHASE_UNIT_ID = "default"
    }

    abstract fun toNativeCheckout() : com.paypal.checkout.order.patch.OrderUpdate
}

/**
 * Possible operations that can be performed on a field.
 */
enum class PatchOperation(val stringValue: String) {
    ADD("add"),
    REPLACE("replace"),
    REMOVE("remove")
}