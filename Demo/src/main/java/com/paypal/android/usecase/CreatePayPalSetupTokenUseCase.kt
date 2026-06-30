package com.paypal.android.usecase

import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.model.serialization.PayPalDetails
import com.paypal.android.api.model.serialization.PayPalExperienceContext
import com.paypal.android.api.model.serialization.PayPalNativeApp
import com.paypal.android.api.model.serialization.PayPalSetupRequestBody
import com.paypal.android.api.model.serialization.PayPalSource
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.utils.ReturnUrlProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreatePayPalSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(): SDKSampleServerResult<PayPalSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            val returnToAppUrlConfig = ReturnUrlProvider.returnToAppUrlConfig
            val experienceContext = PayPalExperienceContext(
                vaultInstruction = "ON_PAYER_APPROVAL",
                returnUrl = returnToAppUrlConfig.returnAppUrl,
                cancelUrl = returnToAppUrlConfig.cancelAppUrl,
                nativeApp = PayPalNativeApp(appUrl = returnToAppUrlConfig.fallbackSchemeUrl)
            )

            val payPalSetupRequest = PayPalSetupRequestBody(
                paymentSource = PayPalSource(
                    paypal = PayPalDetails(
                        usageType = "MERCHANT",
                        experienceContext = experienceContext
                    )
                )
            )

            sdkSampleServerAPI.createPayPalSetupToken(payPalSetupRequest)
        }
}
