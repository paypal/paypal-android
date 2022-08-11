package com.paypal.android.usecase

import com.paypal.android.api.services.SDKSampleServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAccessTokenUseCase @Inject constructor(
    private val sdkSampleServerApi: SDKSampleServerApi
) {
    suspend operator fun invoke(): String = withContext(Dispatchers.IO) {
        val tokenResponse = sdkSampleServerApi.fetchAccessToken()
        tokenResponse.value
    }
}