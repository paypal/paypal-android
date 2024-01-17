package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetClientIdUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(): UseCaseResult<String, Exception> = withContext(Dispatchers.IO) {
        try {
            val clientId = sdkSampleServerAPI.fetchClientId()
            UseCaseResult.Success(clientId)
        } catch (e: Exception) {
            UseCaseResult.Failure(e)
        }
    }
}
