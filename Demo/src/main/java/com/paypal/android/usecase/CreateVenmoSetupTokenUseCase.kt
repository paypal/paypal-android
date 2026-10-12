package com.paypal.android.usecase

import com.paypal.android.api.model.VenmoSetupToken
import com.paypal.android.api.model.serialization.VenmoDetails
import com.paypal.android.api.model.serialization.VenmoSetupRequestBody
import com.paypal.android.api.model.serialization.VenmoSource
import com.paypal.android.api.model.serialization.VenmoVaultExperienceContext
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateVenmoSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(): SDKSampleServerResult<VenmoSetupToken, Exception> =
        withContext(Dispatchers.IO) {
            val experienceContext = VenmoVaultExperienceContext(
                vaultInstruction = "ON_PAYER_APPROVAL"
            )

            val venmoSetupRequest = VenmoSetupRequestBody(
                paymentSource = VenmoSource(
                    venmo = VenmoDetails(
                        usageType = "MERCHANT",
                        experienceContext = experienceContext
                    )
                )
            )

            sdkSampleServerAPI.createVenmoSetupToken(venmoSetupRequest)
        }
}
