package com.paypal.android.usecase

import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupTokenId: String): SDKSampleServerResult<CardSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            sdkSampleServerAPI.getSetupToken(setupTokenId)
        }
}
