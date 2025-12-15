package com.paypal.android.usecase

import com.paypal.android.DemoConstants.APP_FALLBACK_URL_SCHEME
import com.paypal.android.DemoConstants.APP_URL
import com.paypal.android.api.model.DeepLinkStrategy
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.model.serialization.PayPalDetails
import com.paypal.android.api.model.serialization.PayPalExperienceContext
import com.paypal.android.api.model.serialization.PayPalNativeApp
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

    suspend operator fun invoke(
        appSwitchEnabled: Boolean,
        deepLinkStrategy: DeepLinkStrategy
    ): SDKSampleServerResult<PayPalSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            val returnUrl = when (deepLinkStrategy) {
                DeepLinkStrategy.APP_LINK -> "$APP_URL/success"
                DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_FALLBACK_URL_SCHEME/success"
            }
            val cancelUrl = when (deepLinkStrategy) {
                DeepLinkStrategy.APP_LINK -> "$APP_URL/cancel"
                DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_FALLBACK_URL_SCHEME/cancel"
            }

            val experienceContext = PayPalExperienceContext(
                vaultInstruction = "ON_PAYER_APPROVAL",
                returnUrl = returnUrl,
                cancelUrl = cancelUrl,
                nativeApp = PayPalNativeApp(
                    appUrl = APP_URL
                )
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
