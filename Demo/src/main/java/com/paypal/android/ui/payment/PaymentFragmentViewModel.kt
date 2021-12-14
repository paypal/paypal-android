package com.paypal.android.ui.payment

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.Order
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.ui.paypal.OrderUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class PaymentFragmentViewModel @Inject constructor(
    private val payPalDemoApi: PayPalDemoApi
) : ViewModel() {

    private val orderJson = OrderUtils.orderWithShipping
    private lateinit var payPalClient: PayPalClient

    @RequiresApi(Build.VERSION_CODES.M)
    fun startPayPalCheckout() {
        viewModelScope.launch {
            try {
                val order = fetchOrder()
                order.id?.let { orderId ->
                    payPalClient.checkout(orderId) { result ->
                        when (result) {
                            is PayPalCheckoutResult.Success -> Log.i(
                                TAG,
                                "Order Approved: ${result.orderId} && ${result.payerId}"
                            )
                            is PayPalCheckoutResult.Failure -> Log.i(
                                TAG,
                                "Checkout Error: ${result.error.reason}"
                            )
                            is PayPalCheckoutResult.Cancellation -> Log.i(TAG, "User cancelled")
                        }
                    }
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, e.message!!)
            } catch (e: HttpException) {
                Log.e(TAG, e.message!!)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setPayPalClient(payPalClient: PayPalClient) {
        this.payPalClient = payPalClient
    }

    private suspend fun fetchOrder(): Order {
        val orderJson = JsonParser.parseString(orderJson) as JsonObject
        return payPalDemoApi.fetchOrderId(countryCode = "US", orderJson)
    }

    companion object {
        private val TAG = PaymentFragmentViewModel::class.qualifiedName
    }
}
