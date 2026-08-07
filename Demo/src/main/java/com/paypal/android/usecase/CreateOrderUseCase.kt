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
        return withContext(Dispatchers.IO) {
            sdkSampleServerAPI.createOrder(request.toOrderRequestBody())
        }
    }
}

private fun OrderRequest.toOrderRequestBody(): OrderRequestBody {
    val vaultAttributes = shouldVaultOnSuccess
        .takeIf { it }
        ?.let {
            val vault = Vault(
                storeInVault = "ON_SUCCESS",
                usageType = "MERCHANT",
                customerType = "CONSUMER"
            )
            PayPalAttributes(vault = vault)
        }

    val experienceContext = PayPalOrderExperienceContext(
        returnUrl = returnToAppUrlConfig.returnAppUrl,
        cancelUrl = returnToAppUrlConfig.cancelAppUrl,
        userAction = userAction
    )

    val paymentSource = OrderPaymentSource(
        paypal = PayPalPaymentSource(
            attributes = vaultAttributes,
            experienceContext = experienceContext
        )
    )

    val amount = Amount(
        currencyCode = "USD",
        value = "10.99"
    )

    return OrderRequestBody(
        intent = intent,
        purchaseUnits = listOf(PurchaseUnit(amount)),
        paymentSource = paymentSource
    )
}
