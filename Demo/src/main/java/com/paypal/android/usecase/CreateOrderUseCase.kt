package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.serialization.Amount
import com.paypal.android.api.model.serialization.Card
import com.paypal.android.api.model.serialization.CardAttributes
import com.paypal.android.api.model.serialization.OrderPaymentSource
import com.paypal.android.api.model.serialization.OrderRequestBody
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

    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> =
        withContext(Dispatchers.IO) {
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
                paymentSource = if (request.shouldVault) {
                    OrderPaymentSource(
                        card = Card(
                            attributes = CardAttributes(
                                vault = Vault(
                                    storeInVault = "ON_SUCCESS"
                                )
                            )
                        )
                    )
                } else null
            )

            sdkSampleServerAPI.createOrder(orderRequestBody)
        }
}
