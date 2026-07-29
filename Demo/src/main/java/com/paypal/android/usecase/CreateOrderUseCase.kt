package com.paypal.android.usecase

import com.paypal.android.DemoConstants.returnToAppUrlConfig
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.model.serialization.Amount
import com.paypal.android.api.model.serialization.AppSwitchContext
import com.paypal.android.api.model.serialization.NativeApp
import com.paypal.android.api.model.serialization.OrderPaymentSource
import com.paypal.android.api.model.serialization.OrderRequestBody
import com.paypal.android.api.model.serialization.PayPalOrderExperienceContext
import com.paypal.android.api.model.serialization.PayPalPaymentSource
import com.paypal.android.api.model.serialization.PurchaseUnit
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.models.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    // Hard-coded to match XOSphere's PPCP Direct app-switch request exactly (return/cancel URLs
    // excepted, which stay dynamic from returnToAppUrlConfig) so the demo request body is a
    // byte-for-byte reference for comparing against the CreateOrderRequestDto XOSphere sends.
    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> {
        val paymentSource = OrderPaymentSource(
            paypal = PayPalPaymentSource(
                usageType = null,
                emailAddress = "appswitch1@pp.com",
                experienceContext = PayPalOrderExperienceContext(
                    userAction = "CONTINUE",
                    returnUrl = returnToAppUrlConfig.returnAppUrl,
                    cancelUrl = returnToAppUrlConfig.cancelAppUrl,
                    paymentMethodSelected = "PAYPAL_CREDIT",
                    appSwitchContext = AppSwitchContext(
                        nativeApp = NativeApp(
                            appUrl = null,
                            returnAppUrl = returnToAppUrlConfig.returnAppUrl,
                            cancelAppUrl = returnToAppUrlConfig.cancelAppUrl,
                            osType = "ANDROID",
                            osVersion = 36
                        )
                    )
                ),
                usagePattern = null,
                billingPlan = null,
                attributes = null,
                token = null
            )
        )
        return withContext(Dispatchers.IO) {
            val amount = Amount(
                currencyCode = "USD",
                value = "100"
            )

            val purchaseUnit = PurchaseUnit(
                amount = amount
            )

            val orderRequestBody = OrderRequestBody(
                intent = OrderIntent.CAPTURE,
                purchaseUnits = listOf(purchaseUnit),
                paymentSource = paymentSource
            )

            sdkSampleServerAPI.createOrder(orderRequestBody)
        }
    }
}
