package com.paypal.android.usecase

import com.paypal.android.DemoConstants.returnToAppUrlConfig
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.serialization.Amount
import com.paypal.android.api.model.serialization.OrderPaymentSource
import com.paypal.android.api.model.serialization.OrderRequestBody
import com.paypal.android.api.model.serialization.PayPalAttributes
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
        val paymentSource = OrderPaymentSource(
            paypal = PayPalPaymentSource(
                attributes = if (request.shouldVaultOnSuccess) {
                    PayPalAttributes(
                        vault = Vault(
                            storeInVault = "ON_SUCCESS",
                            usageType = "MERCHANT",
                            customerType = "CONSUMER"
                        )
                    )
                } else {
                    null
                },
                experienceContext = PayPalOrderExperienceContext(
                    returnUrl = returnToAppUrlConfig.returnAppUrl,
                    cancelUrl = returnToAppUrlConfig.cancelAppUrl,
                    paymentMethodSelected = request.paymentMethodSelected,
                    userAction = request.userAction
                )
            )
        )
        return withContext(Dispatchers.IO) {
            val amount = Amount(
                currencyCode = "USD",
                value = request.amount
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
