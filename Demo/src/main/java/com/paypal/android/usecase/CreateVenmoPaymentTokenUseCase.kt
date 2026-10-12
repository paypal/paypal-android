package com.paypal.android.usecase

import com.paypal.android.api.model.VenmoPaymentToken
import com.paypal.android.api.model.VenmoSetupToken
import com.paypal.android.api.model.serialization.setupTokenPaymentRequest
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateVenmoPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(setupToken: VenmoSetupToken): SDKSampleServerResult<VenmoPaymentToken, Exception> =
        withContext(Dispatchers.IO) {
            sdkSampleServerAPI.createVenmoPaymentToken(setupTokenPaymentRequest(setupToken.id))
        }
}
