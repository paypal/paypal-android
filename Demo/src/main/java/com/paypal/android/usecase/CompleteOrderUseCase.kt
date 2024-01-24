package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CompleteOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(
        orderId: String,
        intent: OrderIntent,
        clientMetadataId: String
    ): SDKSampleServerResult<Order, Exception> = withContext(Dispatchers.IO) {
        when (intent) {
            OrderIntent.CAPTURE ->
                sdkSampleServerAPI.captureOrder(orderId, clientMetadataId)

            OrderIntent.AUTHORIZE ->
                sdkSampleServerAPI.authorizeOrder(orderId, clientMetadataId)
        }
    }
}
