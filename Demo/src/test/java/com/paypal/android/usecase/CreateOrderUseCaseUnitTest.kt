package com.paypal.android.usecase

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.model.serialization.OrderRequestBody
import com.paypal.android.models.OrderRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CreateOrderUseCaseUnitTest {

    @Test
    fun `valid user actions create a non-vault PayPal payment source and serialize camel case`() {
        listOf("PAY_NOW", "CONTINUE").forEach { userAction ->
            val requestBody = OrderRequest(
                intent = OrderIntent.CAPTURE,
                shouldVaultOnSuccess = false,
                userAction = userAction
            ).toOrderRequestBody()

            val paypal = requestBody.paymentSource?.paypal
            assertNotNull(paypal)
            assertNull(paypal?.attributes)
            assertEquals(userAction, paypal?.experienceContext?.userAction)
            assertEquals("10.99", requestBody.purchaseUnits.single().amount.value)

            val json = Json.encodeToJsonElement(OrderRequestBody.serializer(), requestBody).jsonObject
            assertFalse(json.containsKey("payment_source"))
            val paymentSource = json.getValue("paymentSource").jsonObject
            val paypalJson = paymentSource.getValue("paypal").jsonObject
            assertFalse(paypalJson.containsKey("experience_context"))
            val experienceContext = paypalJson.getValue("experienceContext").jsonObject
            assertEquals(userAction, experienceContext.getValue("userAction").jsonPrimitive.content)
            assertFalse(experienceContext.containsKey("user_action"))
        }
    }

    @Test
    fun `null user action does not introduce a payment source for non-vault callers`() {
        val requestBody = OrderRequest(
            intent = OrderIntent.AUTHORIZE,
            shouldVaultOnSuccess = false
        ).toOrderRequestBody()

        assertNull(requestBody.paymentSource)
        val json = Json.encodeToJsonElement(OrderRequestBody.serializer(), requestBody).jsonObject
        assertFalse(json.containsKey("paymentSource"))
        assertFalse(json.containsKey("payment_source"))
    }

    @Test
    fun `vault callers preserve PayPal attributes when user action is null`() {
        val requestBody = OrderRequest(
            intent = OrderIntent.AUTHORIZE,
            shouldVaultOnSuccess = true
        ).toOrderRequestBody()

        val paypal = requestBody.paymentSource?.paypal
        assertEquals("ON_SUCCESS", paypal?.attributes?.vault?.storeInVault)
        assertNull(paypal?.experienceContext?.userAction)

        val json = Json.encodeToJsonElement(OrderRequestBody.serializer(), requestBody).jsonObject
        val experienceContext = json.getValue("paymentSource")
            .jsonObject.getValue("paypal")
            .jsonObject.getValue("experienceContext")
            .jsonObject
        assertFalse(experienceContext.containsKey("userAction"))
        assertFalse(experienceContext.containsKey("user_action"))
    }
}
