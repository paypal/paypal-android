package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetClientIdUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(): SDKSampleServerResult<String, Exception> = withContext(Dispatchers.IO) {
        sdkSampleServerAPI.fetchClientId()
    }
}
