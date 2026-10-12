package com.paypal.android.usecase

import com.paypal.android.DemoConstants
import com.paypal.android.api.model.VenmoSetupToken
import com.paypal.android.api.model.serialization.VenmoAppSwitchContext
import com.paypal.android.api.model.serialization.VenmoDetails
import com.paypal.android.api.model.serialization.VenmoSetupCustomer
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
            val returnToAppUrlConfig = DemoConstants.returnToAppUrlConfig
            val experienceContext = VenmoVaultExperienceContext(
                returnUrl = returnToAppUrlConfig.returnAppUrl,
                cancelUrl = returnToAppUrlConfig.cancelAppUrl,
                brandName = "AndroidSDK Demo",
                userAction = "SETUP_NOW",
                appSwitchContext = VenmoAppSwitchContext(source = "NATIVE_APP")
            )

            val venmoSetupRequest = VenmoSetupRequestBody(
                paymentSource = VenmoSource(
                    venmo = VenmoDetails(
                        experienceContext = experienceContext,
                        usageType = "MERCHANT",
                        customerType = "CONSUMER",
                        permitMultiplePaymentTokens = false
                    )
                ),
                customer = VenmoSetupCustomer(merchantCustomerId = "demo-customer-id")
            )

            sdkSampleServerAPI.createVenmoSetupToken(venmoSetupRequest)
        }
}
