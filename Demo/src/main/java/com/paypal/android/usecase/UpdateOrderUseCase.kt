package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.utils.OrderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(
        orderId: String,
        shippingMethod: PayPalNativeShippingMethod
    ): UseCaseResult<Boolean, Exception> =
        withContext(Dispatchers.IO) {
            // https://developer.paypal.com/docs/api/orders/v2/#orders_patch
            val options = OrderUtils.createShippingOptionsBuilder(selectedId = shippingMethod.id)
            val patchShipping = PatchRequestBody(
                path = "/purchase_units/@reference_id=='PUHF'/shipping/options",
                value = options
            )
            val amount =
                OrderUtils.getAmount(value = "5.0", shippingValue = shippingMethod.value ?: "0.0")
            val patchAmount = PatchRequestBody(
                path = "/purchase_units/@reference_id=='PUHF'/amount",
                value = amount
            )
            sdkSampleServerAPI.patchOrder(orderId, listOf(patchAmount, patchShipping))
        }

    data class PatchRequestBody(
        val op: String = "replace",
        val path: String,
        val value: Any
    )
}
