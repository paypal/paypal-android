package com.paypal.android.usecase

import com.paypal.android.DemoConstants
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.model.serialization.CardDetails
import com.paypal.android.api.model.serialization.CardPaymentSource
import com.paypal.android.api.model.serialization.CardSetupRequest
import com.paypal.android.api.model.serialization.ExperienceContext
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.uishared.enums.DeepLinkStrategy
import com.paypal.android.utils.ReturnUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(sca: SCA, deepLinkStrategy: DeepLinkStrategy): SDKSampleServerResult<CardSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            // create a payment token with an empty card attribute; the merchant app will
            // provide the card's details through the SDK
            val cardSetupRequest = CardSetupRequest(
                paymentSource = CardPaymentSource(
                    card = CardDetails(
                        verificationMethod = sca.name,
                        experienceContext = ExperienceContext(
                            returnUrl = ReturnUrlFactory.createVaultSuccessUrl(deepLinkStrategy),
                            cancelUrl = ReturnUrlFactory.createVaultCancelUrl(deepLinkStrategy)
                        )
                    )
                )
            )

            sdkSampleServerAPI.createSetupToken(cardSetupRequest)
        }
}
