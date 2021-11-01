package com.paypal.android.ui.paypal

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.paypal.android.api.model.Order
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.checkout.PayPalConfiguration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.gson.JsonParser
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.UserAction
import com.paypal.android.checkout.OrderIntent
import com.paypal.android.checkout.PayPalCheckoutResult
import retrofit2.HttpException

@HiltViewModel
class PayPalViewModel @Inject constructor(
    private val payPalDemoApi: PayPalDemoApi,
) : ViewModel() {

    companion object {
        private val TAG = PayPalViewModel::class.qualifiedName
    }

    private val _userAction = MutableLiveData(UserAction.PAY_NOW)
    val userAction: LiveData<String> =
        Transformations.map(_userAction) { action -> action.name.replace("_", " ") }

    private val _orderIntent = MutableLiveData(OrderIntent.CAPTURE)
    val orderIntent: LiveData<String> =
        Transformations.map(_orderIntent) { intent -> intent.name.replace("_", " ") }

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
                                "Order Approved: ${result.approval}"
                            )
                            is PayPalCheckoutResult.Failure -> Log.i(
                                TAG,
                                "Checkout Error: ${result.error.reason}"
                            )
                            is PayPalCheckoutResult.ShippingChange -> Log.i(
                                TAG,
                                "Shipping Changed: ${result.shippingChangeData}"
                            )
                            is PayPalCheckoutResult.Cancellation -> Log.i(TAG, "User cancelled")
                        }
                    }
                }
            } catch (e: HttpException) {
                Log.e(TAG, e.message!!)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setPayPalConfig(paypalConfig: PayPalConfiguration) {
        payPalClient = PayPalClient(paypalConfig)
    }

    private suspend fun fetchOrder(): Order {
        val orderJson = JsonParser.parseString(
            String.format(
                orderJson,
                _userAction.value?.name,
                _orderIntent.value?.name
            )
        ) as JsonObject
        return payPalDemoApi.fetchOrderId(countryCode = "US", orderJson)
    }

    fun userActionSelected(action: String) {
        _userAction.value = UserAction.valueOf(action.replace(" ", "_"))
    }

    fun orderIntentSelected(intent: String) {
        _orderIntent.value = OrderIntent.valueOf(intent.replace(" ", "_"))
    }
}
