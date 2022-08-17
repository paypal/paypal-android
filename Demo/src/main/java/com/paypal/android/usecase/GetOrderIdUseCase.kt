package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.services.SDKSampleServerApi
import com.paypal.android.ui.paypal.OrderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetOrderIdUseCase@Inject constructor(
    private val sdkSampleServerApi: SDKSampleServerApi
) {

    suspend operator fun invoke(): String? = withContext(Dispatchers.IO) {
        val jsonOrder = JsonParser.parseString(OrderUtils.orderWithShipping) as JsonObject
        val result = sdkSampleServerApi.createOrder(jsonOrder)
        result.id
    }
}
