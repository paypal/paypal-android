package com.paypal.android.usecase


import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.utils.OrderUtils
import com.paypal.checkout.order.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateOrderUseCase @Inject constructor(
    private val sdkSampleServerApi: SDKSampleServerApi
) {
    suspend operator fun invoke(orderID: String, shippingMethod: PayPalNativeShippingMethod) = withContext(Dispatchers.IO) {
        val options = OrderUtils.createShippingOptionsBuilder(selectedId = shippingMethod.id)
        val patchShipping = PatchRequestBody(
            path = "/purchase_units/@reference_id=='PUHF'/shipping/options",
            value = options
        )
        val amount = OrderUtils.getAmount(value = "5.0", shippingValue = shippingMethod.value ?: "0.0")
        val patchAmount = PatchRequestBody(
            path = "/purchase_units/@reference_id=='PUHF'/amount",
            value = amount
        )
        sdkSampleServerApi.patchOrder(orderID, listOf(patchAmount, patchShipping))
    }

    data class PatchRequestBody (
        val op: String = "replace",
        val path: String,
        val value: Any
    )
}