package com.paypal.android.corepayments

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.contract.ApiTaskResult
import org.json.JSONArray
import org.json.JSONObject

class GooglePayClient(
    private val googlePayAPI: GooglePayAPI,
    private val paymentsClient: PaymentsClient
) {
    constructor(context: Context, config: CoreConfig) : this(
        googlePayAPI = GooglePayAPI(context, config),
        paymentsClient = createPaymentsClient(context)
    )

    suspend fun start(): Task<PaymentData> {
        return when (val result = googlePayAPI.getGooglePayConfig()) {
            is GetGooglePayConfigResult.Failure -> TODO("handle error")
            is GetGooglePayConfigResult.Success -> {
                loadPaymentData(result.value)
            }
        }
    }

    private suspend fun loadPaymentData(googlePayConfig: GooglePayConfig): Task<PaymentData> {
        if (googlePayConfig.isEligible) {
            val paymentDataRequestJSON = createPaymentDataRequest(googlePayConfig)
            val paymentDataRequest = PaymentDataRequest.fromJson(paymentDataRequestJSON.toString())
            return paymentsClient.loadPaymentData(paymentDataRequest)
        } else {
            TODO("Handle GooglePay ineligibility")
        }
    }

    private fun createPaymentDataRequest(googlePayConfig: GooglePayConfig): JSONObject {
        val displayItems = JSONArray(
            listOf(
                JSONObject()
                    .put("label", "Subtotal")
                    .put("type", "SUBTOTAL")
                    .put("price", "100.00"),
                JSONObject()
                    .put("label", "Tax")
                    .put("type", "TAX")
                    .put("price", "10.00"),
            )
        )

        val transactionInfo = JSONObject()
            .put("displayItems", displayItems)
            .put("countryCode", "US")
            .put("currencyCode", "USD")
            .put("totalPriceStatus", "FINAL")
            .put("totalPrice", "110.00")
            .put("totalPriceLabel", "Total")

        val request = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("allowedPaymentMethods", googlePayConfig.allowedPaymentMethods)
            .put("merchantInfo", googlePayConfig.merchantInfo)
            .put("callbackIntents", JSONArray(listOf("PAYMENT_AUTHORIZATION")))
            .put("transactionInfo", transactionInfo)
        return request
    }

    suspend fun confirmOrder(
        orderId: String,
        taskResult: ApiTaskResult<PaymentData>
    ): ApproveGooglePayPaymentResult {
        val paymentData = taskResult.result
        if (paymentData == null) {
            val error = PayPalSDKError(0, "ApiTaskResult has no payment data.")
            return ApproveGooglePayPaymentResult.Failure(error)
        }

        val paymentDataJSON = JSONObject(paymentData.toJson())
        val paymentMethodData = paymentDataJSON.getJSONObject("paymentMethodData")
        return googlePayAPI.confirmOrder(
            orderId = orderId,
            paymentMethodData = paymentMethodData
        )
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