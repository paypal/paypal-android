package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.ui.paypal.ShippingPreferenceType
import com.paypal.android.utils.OrderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetOrderIdUseCase@Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(shippingPreferenceType: ShippingPreferenceType): String? =
        withContext(Dispatchers.IO) {
            val order = OrderUtils.createOrderBuilder(
                "5.0",
                shippingPreference = shippingPreferenceType.nxoShippingPreference
            )
            val result = sdkSampleServerAPI.createOrder(order)
            result.id
        }
}
