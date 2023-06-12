package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthorizeOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(orderId: String) = withContext(Dispatchers.IO) {
        sdkSampleServerAPI.authorizeOrder(orderId)
    }
}
