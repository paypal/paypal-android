package com.paypal.android.ui.paypal

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.paypal.android.api.model.Order
import com.paypal.android.checkout.PayPalClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.gson.JsonParser
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalRequest
import com.paypal.android.checkout.pojo.CorrelationIds
import com.paypal.android.checkout.pojo.ErrorInfo
import retrofit2.HttpException
import java.net.UnknownHostException

@HiltViewModel
class PayPalViewModel @Inject constructor(
    private val payPalDemoApi: PayPalDemoApi
) : ViewModel() {

    companion object {
        private val TAG = PayPalViewModel::class.qualifiedName
    }

    private val orderJson = OrderUtils.orderWithShipping
    private var payPalClient: PayPalClient? = null

    private val _checkoutResult = MutableLiveData<PayPalCheckoutResult>()
    val checkoutResult: LiveData<PayPalCheckoutResult> = _checkoutResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun startPayPalCheckout() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val order = fetchOrder()
                order.id?.let { orderId ->
                    val payPalRequest = PayPalRequest(orderId)
                    payPalClient?.approveOrder(payPalRequest) { result ->
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
                        _checkoutResult.value = result
                        _isLoading.value = false
                    }
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, e.message!!)
                val error = PayPalCheckoutResult.Failure(error = ErrorInfo(e, e.message!!, CorrelationIds(), null))
                _checkoutResult.value = error
                _isLoading.value = false
            } catch (e: HttpException) {
                Log.e(TAG, e.message!!)
                val error = PayPalCheckoutResult.Failure(error = ErrorInfo(e, e.message!!, CorrelationIds(), null))
                _checkoutResult.value = error
                _isLoading.value = false
            }
        }
    }

    fun setPayPalClient(payPalClient: PayPalClient) {
        this.payPalClient = payPalClient
    }

    fun handlePayPalBrowserSwitchResult(activity: FragmentActivity) {
        payPalClient?.handleBrowserSwitchResult(activity)
    }

    private suspend fun fetchOrder(): Order {
        val orderJson = JsonParser.parseString(orderJson) as JsonObject
        return payPalDemoApi.fetchOrderId(countryCode = "US", orderJson)
    }
}
