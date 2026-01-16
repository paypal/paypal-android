package com.paypal.android.corepayments

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.contract.ApiTaskResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            is GetGooglePayConfigResult.Failure -> {
                Log.e(TAG, "Failed to get Google Pay config: ${result.error}")
                Tasks.forException(
                    Exception("Failed to get Google Pay config: ${result.error.errorDescription}")
                )
            }

            is GetGooglePayConfigResult.Success -> {
                loadPaymentData(result.value)
            }
        }
    }

    private fun loadPaymentData(googlePayConfig: GooglePayConfig): Task<PaymentData> {
        if (googlePayConfig.isEligible) {
            val paymentDataRequestJSON = createPaymentDataRequest(googlePayConfig)
            val paymentDataRequest = PaymentDataRequest.fromJson(paymentDataRequestJSON.toString())
            return paymentsClient.loadPaymentData(paymentDataRequest)
        } else {
            Log.e(TAG, "Google Pay is not eligible for this transaction")
            return Tasks.forException(
                Exception("Google Pay is not eligible for this transaction")
            )
        }
    }

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

        // Convert typed data to JSON for Google SDK
        val json = Json { ignoreUnknownKeys = true }
        val allowedPaymentMethodsJSON = googlePayConfig.allowedPaymentMethods?.let {
            JSONArray(json.encodeToString(it))
        }
        val merchantInfoJSON = googlePayConfig.merchantInfo?.let {
            JSONObject(json.encodeToString(it))
        }

        val request = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("allowedPaymentMethods", allowedPaymentMethodsJSON)
            .put("merchantInfo", merchantInfoJSON)
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

        // Parse Google Pay response to typed data
        val paymentDataJSON = JSONObject(paymentData.toJson())
        val paymentMethodDataJSON = paymentDataJSON.getJSONObject("paymentMethodData")

        val json = Json { ignoreUnknownKeys = true }
        val paymentMethodData = try {
            json.decodeFromString<GooglePayPaymentMethodData>(paymentMethodDataJSON.toString())
        } catch (e: Exception) {
            val error = PayPalSDKError(
                code = 0,
                errorDescription = "Failed to parse payment method data: ${e.message}",
                reason = e
            )
            return ApproveGooglePayPaymentResult.Failure(error)
        }

        return googlePayAPI.confirmOrder(
            orderId = orderId,
            paymentMethodData = paymentMethodData
        )
    }

    companion object {
        private const val TAG = "GooglePayClient"

        fun createPaymentsClient(context: Context): PaymentsClient {
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()

            return Wallet.getPaymentsClient(context, walletOptions)
        }
    }
}