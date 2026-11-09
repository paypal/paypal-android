package com.paypal.android.usecase

import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.model.serialization.setupTokenPaymentRequest
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateCardPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupToken: CardSetupToken): SDKSampleServerResult<CardPaymentToken, Exception> =
        withContext(Dispatchers.IO) {
            sdkSampleServerAPI.createPaymentToken(setupTokenPaymentRequest(setupToken.id))
        }
}
