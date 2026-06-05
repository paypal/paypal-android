package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.serialization.Amount
import com.paypal.android.api.model.serialization.ApplicationContext
import com.paypal.android.api.model.serialization.OrderPaymentSource
import com.paypal.android.api.model.serialization.OrderRequestBody
import com.paypal.android.api.model.serialization.PurchaseUnit
import com.paypal.android.api.model.serialization.VenmoAppSwitchContext
import com.paypal.android.api.model.serialization.VenmoApplicationContext
import com.paypal.android.api.model.serialization.VenmoExperienceContext
import com.paypal.android.api.model.serialization.VenmoPaymentSource
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.models.OrderRequest
import com.paypal.android.utils.ReturnUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateVenmoOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> {
        val paymentSource = when {
            request.appSwitchWhenEligible -> {
                val returnToAppStrategy = request.returnToAppStrategy.toReturnToAppStrategy()
                OrderPaymentSource(
                    venmo = VenmoPaymentSource(
                        experienceContext = VenmoExperienceContext(
                            returnUrl = ReturnUrlFactory.createCheckoutSuccessUrl(
                                returnToAppStrategy
                            ),
                            cancelUrl = ReturnUrlFactory.createCheckoutCancelUrl(returnToAppStrategy),
                            appSwitchContext = VenmoAppSwitchContext(source = "NATIVE_APP")
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
