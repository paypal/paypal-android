package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAccessTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(): String = withContext(Dispatchers.IO) {
        val tokenResponse = sdkSampleServerAPI.fetchAccessToken()
        tokenResponse.value
    }
}
