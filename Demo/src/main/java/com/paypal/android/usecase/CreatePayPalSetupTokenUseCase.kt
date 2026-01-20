package com.paypal.android.usecase

import com.paypal.android.DemoConstants.APP_CUSTOM_URL_SCHEME
import com.paypal.android.DemoConstants.APP_URL
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.model.serialization.PayPalDetails
import com.paypal.android.api.model.serialization.PayPalExperienceContext
import com.paypal.android.api.model.serialization.PayPalNativeApp
import com.paypal.android.api.model.serialization.PayPalSetupRequestBody
import com.paypal.android.api.model.serialization.PayPalSource
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.uishared.enums.DeepLinkStrategy
import com.paypal.android.utils.ReturnUrlFactory
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

            val appUrl = when (deepLinkStrategy) {
                DeepLinkStrategy.APP_LINKS -> APP_URL
                DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_CUSTOM_URL_SCHEME://"
            }
            val experienceContext = PayPalExperienceContext(
                vaultInstruction = "ON_PAYER_APPROVAL",
                returnUrl = ReturnUrlFactory.createCheckoutSuccessUrl(deepLinkStrategy),
                cancelUrl = ReturnUrlFactory.createCheckoutCancelUrl(deepLinkStrategy),
                nativeApp = PayPalNativeApp(appUrl = appUrl)
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
