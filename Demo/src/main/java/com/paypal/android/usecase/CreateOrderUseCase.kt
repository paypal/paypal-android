package com.paypal.android.usecase

import com.paypal.android.DemoConstants.APP_URL
import com.paypal.android.DemoConstants.APP_CUSTOM_URL_SCHEME
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.serialization.Amount
import com.paypal.android.api.model.serialization.Card
import com.paypal.android.api.model.serialization.CardAttributes
import com.paypal.android.api.model.serialization.NativeApp
import com.paypal.android.api.model.serialization.OrderPaymentSource
import com.paypal.android.api.model.serialization.OrderRequestBody
import com.paypal.android.api.model.serialization.PayPalOrderExperienceContext
import com.paypal.android.api.model.serialization.PayPalPaymentSource
import com.paypal.android.api.model.serialization.PurchaseUnit
import com.paypal.android.api.model.serialization.Vault
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.models.OrderRequest
import com.paypal.android.uishared.enums.DeepLinkStrategy
import com.paypal.android.utils.ReturnUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> {
        val paymentSource = when {
            request.appSwitchWhenEligible -> {
                val deepLinkStrategy = request.deepLinkStrategy
                val appUrl = when (deepLinkStrategy) {
                    DeepLinkStrategy.APP_LINKS -> APP_URL
                    DeepLinkStrategy.CUSTOM_URL_SCHEME -> "${APP_CUSTOM_URL_SCHEME}://"
                }
                OrderPaymentSource(
                    paypal = PayPalPaymentSource(
                        experienceContext = PayPalOrderExperienceContext(
                            returnUrl = ReturnUrlFactory.createCheckoutSuccessUrl(deepLinkStrategy),
                            cancelUrl = ReturnUrlFactory.createCheckoutCancelUrl(deepLinkStrategy),
                            nativeApp = NativeApp(appUrl = appUrl)
                        )
                    )
                )
            }

            request.shouldVaultOnSuccess -> {
                OrderPaymentSource(
                    card = Card(attributes = CardAttributes(vault = Vault(storeInVault = "ON_SUCCESS")))
                )
            }

            else -> null
        }
        return withContext(Dispatchers.IO) {
            val amount = Amount(
                currencyCode = "USD",
                value = "10.99"
            )

            val purchaseUnit = PurchaseUnit(
                amount = amount
            )

            val orderRequestBody = OrderRequestBody(
                intent = request.intent,
                purchaseUnits = listOf(purchaseUnit),
                paymentSource = paymentSource
            )

            sdkSampleServerAPI.createOrder(orderRequestBody)
        }
    }
}
