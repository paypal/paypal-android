package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.ui.paypalnative.ShippingPreferenceType
import com.paypal.android.utils.OrderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(
        shippingPreferenceType: ShippingPreferenceType,
        orderIntent: OrderIntent
    ): UseCaseResult<Order, Exception> = withContext(Dispatchers.IO) {
        val orderRequest = OrderUtils.createOrderBuilder(
            "5.0",
            orderIntent = orderIntent,
            shippingPreference = shippingPreferenceType.nxoShippingPreference
        )
        sdkSampleServerAPI.createOrder(orderRequest)
    }
}
