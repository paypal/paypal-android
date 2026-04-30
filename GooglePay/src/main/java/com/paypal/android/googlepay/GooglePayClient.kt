package com.paypal.android.googlepay

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.paypal.android.corepayments.CoreConfig
import kotlinx.serialization.InternalSerializationApi
import org.json.JSONArray
import org.json.JSONObject

class GooglePayClient internal constructor(
    private val googlePayAPI: GooglePayAPI,
    private val paymentsClient: PaymentsClient
) {

    constructor(context: Context, config: CoreConfig) : this(
        googlePayAPI = GooglePayAPI(context, config),
        paymentsClient = createPaymentsClient(context)
    )

    @OptIn(InternalSerializationApi::class)
    suspend fun start(): SDKResult<GooglePayLaunchRequest> {
        return when (val result = googlePayAPI.getGooglePayConfig()) {
            is SDKResult.Success -> {
                val config = result.value
                val loadPaymentDataTask = loadPaymentData(config)
                SDKResult.Success(GooglePayLaunchRequest(loadPaymentDataTask))
            }

            // forward result
            is SDKResult.Failure -> result
        }
    }

    @OptIn(InternalSerializationApi::class)
    private suspend fun loadPaymentData(googlePayConfig: GooglePayConfig): Task<PaymentData> {
        if (googlePayConfig.isEligible) {
            val paymentDataRequestJSON = createPaymentDataRequest(googlePayConfig)
            val paymentDataRequest = PaymentDataRequest.fromJson(paymentDataRequestJSON.toString())
            return paymentsClient.loadPaymentData(paymentDataRequest).awaitTask()
        } else {
            TODO("Handle GooglePay ineligibility")
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun createPaymentDataRequest(googlePayConfig: GooglePayConfig): JSONObject {
        val displayItems = JSONArray(
            listOf(
                JSONObject()
                    .put("label", "Subtotal")
                    .put("type", "SUBTOTAL")
                    .put("price", "1.00"),
                JSONObject()
                    .put("label", "Tax")
                    .put("type", "TAX")
                    .put("price", "0.00"),
            )
        )

        val transactionInfo = JSONObject()
            .put("displayItems", displayItems)
            .put("countryCode", "US")
            .put("currencyCode", "USD")
            .put("totalPriceStatus", "FINAL")
            .put("totalPrice", "1.00")
            .put("totalPriceLabel", "Total")

        // TODO: migrate to kotlinx serialization so we don't have to make this conversion
        val allowedPaymentMethods =
            googlePayConfig.allowedPaymentMethods?.let { JSONArray(it.toString()) }
        val merchantInfo = googlePayConfig.merchantInfo?.let { JSONObject(it.toString()) }

        val request = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("merchantInfo", merchantInfo)
            .put("allowedPaymentMethods", allowedPaymentMethods)
            .put("callbackIntents", JSONArray(listOf("PAYMENT_AUTHORIZATION")))
            .put("transactionInfo", transactionInfo)
        return request
    }

    companion object {
        fun createPaymentsClient(context: Context): PaymentsClient {
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()

            return Wallet.getPaymentsClient(context, walletOptions)
        }
    }
}
