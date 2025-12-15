package com.paypal.android.usecase

import com.paypal.android.DemoConstants.APP_FALLBACK_URL_SCHEME
import com.paypal.android.DemoConstants.APP_URL
import com.paypal.android.api.model.DeepLinkStrategy
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> {
        val returnUrl = when (request.deepLinkStrategy) {
            DeepLinkStrategy.APP_LINK -> "$APP_URL/success"
            DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_FALLBACK_URL_SCHEME/success"
        }
        val cancelUrl = when (request.deepLinkStrategy) {
            DeepLinkStrategy.APP_LINK -> "$APP_URL/cancel"
            DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_FALLBACK_URL_SCHEME/cancel"
        }

        val paymentSource = when {
            request.appSwitchWhenEligible -> {
                OrderPaymentSource(
                    paypal = PayPalPaymentSource(
                        experienceContext = PayPalOrderExperienceContext(
                            returnUrl = returnUrl,
                            cancelUrl = cancelUrl,
                            nativeApp = NativeApp(appUrl = APP_URL)
                        )
                    )
                )
            }

            request.shouldVault -> {
                OrderPaymentSource(
                    card = Card(
                        attributes = CardAttributes(
                            vault = Vault(
                                storeInVault = "ON_SUCCESS"
                            )
                        )
                    )
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
