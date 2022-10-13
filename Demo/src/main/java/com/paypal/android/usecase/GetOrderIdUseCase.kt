package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.utils.OrderUtils
import com.paypal.android.ui.paypal.ShippingPreferenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetOrderIdUseCase@Inject constructor(
    private val sdkSampleServerApi: SDKSampleServerApi
) {

    suspend operator fun invoke(shippingPreferenceType: ShippingPreferenceType): String? =
        withContext(Dispatchers.IO) {
            val order = OrderUtils.createOrderBuilder(
                "100.0",
                shippingPreference = shippingPreferenceType.nxoShippingPreference
            )
            val result = sdkSampleServerApi.createOrder(order)
            result.id
        }
}
