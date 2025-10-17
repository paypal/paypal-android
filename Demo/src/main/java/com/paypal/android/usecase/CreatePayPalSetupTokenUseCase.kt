package com.paypal.android.usecase

import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.model.serialization.PayPalDetails
import com.paypal.android.api.model.serialization.PayPalExperienceContext
import com.paypal.android.api.model.serialization.PayPalSetupRequestBody
import com.paypal.android.api.model.serialization.PayPalSource
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(): SDKSampleServerResult<PayPalSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            val payPalSetupRequest = PayPalSetupRequestBody(
                paymentSource = PayPalSource(
                    paypal = PayPalDetails(
                        usageType = "MERCHANT",
                        experienceContext = PayPalExperienceContext(
                            vaultInstruction = "ON_PAYER_APPROVAL",
                            returnUrl = "com.paypal.android.demo://vault/success",
                            cancelUrl = "com.paypal.android.demo://vault/cancel"
                        )
                    )
                )
            )

            sdkSampleServerAPI.createPayPalSetupToken(payPalSetupRequest)
        }
}
