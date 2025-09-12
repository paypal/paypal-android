package com.paypal.android.usecase

import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.model.serialization.PaymentSource
import com.paypal.android.api.model.serialization.Token
import com.paypal.android.api.model.serialization.TokenRequest
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreatePayPalPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(setupToken: PayPalSetupToken): SDKSampleServerResult<PayPalPaymentToken, Exception> =
        withContext(Dispatchers.IO) {
            val tokenRequest = TokenRequest(
                paymentSource = PaymentSource(
                    token = Token(
                        id = setupToken.id,
                        type = "SETUP_TOKEN"
                    )
                )
            )

            sdkSampleServerAPI.createPayPalPaymentToken(tokenRequest)
        }
}
