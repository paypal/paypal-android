package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.ui.paypal.OrderUtils
import com.paypal.android.ui.paypal.ShippingPreferenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetOrderIdUseCase@Inject constructor(
    private val sdkSampleServerApi: SDKSampleServerApi
) {

    suspend operator fun invoke(shippingPreferenceType: ShippingPreferenceType): String? =
        withContext(Dispatchers.IO) {
            val orderRequestString = when (shippingPreferenceType) {
                ShippingPreferenceType.GET_FROM_FILE -> OrderUtils.orderWithShipping
                ShippingPreferenceType.NO_SHIPPING -> OrderUtils.orderWithoutShipping
                ShippingPreferenceType.SET_PROVIDED_ADDRESS -> OrderUtils.orderWithProvidedAddress
            }
            val jsonOrder = JsonParser.parseString(orderRequestString) as JsonObject
            val result = sdkSampleServerApi.createOrder(jsonOrder)
            result.id
        }
}
