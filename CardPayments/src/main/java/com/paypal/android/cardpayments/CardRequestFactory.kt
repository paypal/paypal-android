package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.api.ApplicationContext
import com.paypal.android.cardpayments.api.BillingAddress
import com.paypal.android.cardpayments.api.CardAttributes
import com.paypal.android.cardpayments.api.CardPaymentSource
import com.paypal.android.cardpayments.api.ConfirmPaymentSourceRequest
import com.paypal.android.cardpayments.api.PaymentSource
import com.paypal.android.cardpayments.api.Verification
import com.paypal.android.cardpayments.api.VerificationMethod
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.HttpMethod
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class CardRequestFactory {
    @OptIn(InternalSerializationApi::class)
    fun createConfirmPaymentSourceRequest(cardRequest: CardRequest): APIRequest {
        val card = cardRequest.card
        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val billingAddress = card.billingAddress?.let { address ->
            BillingAddress(
                addressLine1 = address.streetAddress,
                addressLine2 = address.extendedAddress,
                adminArea2 = address.locality,
                adminArea1 = address.region,
                postalCode = address.postalCode,
                countryCode = address.countryCode
            )
        }

        val verification = when (cardRequest.sca) {
            SCA.SCA_WHEN_REQUIRED -> Verification(method = VerificationMethod.SCA_WHEN_REQUIRED)
            SCA.SCA_ALWAYS -> Verification(method = VerificationMethod.SCA_ALWAYS)
        }

        val cardAttributes = CardAttributes(verification = verification)

        val cardPaymentSource = CardPaymentSource(
            name = card.cardholderName,
            number = cardNumber,
            expiry = cardExpiry,
            securityCode = card.securityCode,
            billingAddress = billingAddress,
            attributes = cardAttributes
        )

        val paymentSource = PaymentSource(card = cardPaymentSource)

        val applicationContext = ApplicationContext(
            returnUrl = cardRequest.returnUrl,
            cancelUrl = cardRequest.returnUrl // we can set the same url
        )

        val confirmPaymentSourceRequest = ConfirmPaymentSourceRequest(
            paymentSource = paymentSource,
            applicationContext = applicationContext
        )

        val body = Json.encodeToString(confirmPaymentSourceRequest)

        return APIRequest(
            path = "v2/checkout/orders/${cardRequest.orderId}/confirm-payment-source",
            method = HttpMethod.POST,
            body = body
        )
    }
}
