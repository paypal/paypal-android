package com.paypal.android.ui.paypal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.paypal.android.api.model.Order
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.checkout.PayPalClientListener
import com.paypal.android.checkout.PayPalConfiguration
import com.paypal.android.checkout.pojo.CorrelationIds
import com.paypal.android.checkout.pojo.ErrorInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.gson.JsonParser
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.UserAction
import com.paypal.android.checkout.OrderIntent
import com.paypal.android.checkout.pojo.Approval
import com.paypal.android.checkout.pojo.ShippingChangeData
import com.paypal.android.checkout.shipping.ShippingChangeActions
import retrofit2.HttpException

@HiltViewModel
class PayPalViewModel @Inject constructor(
    private val payPalDemoApi: PayPalDemoApi,
) : ViewModel(), PayPalClientListener {

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

    fun startPayPalCheckout() {
        viewModelScope.launch {
            try {
                val order = fetchOrder()
                order.id?.let { orderId ->
                    payPalClient.checkout(orderId, this@PayPalViewModel)
                }
            } catch (e: HttpException) {
                onPayPalError(
                    ErrorInfo(
                        error = e,
                        reason = e.message!!,
                        correlationIds = CorrelationIds(),
                        orderId = "",
                    )
                )
                Log.e(TAG, e.message!!)
            }
        }
    }

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

    override fun onPayPalApprove(approval: Approval) {
        Log.i(TAG, "Order Approved: $approval")
    }

    override fun onPayPalError(errorInfo: ErrorInfo) {
        Log.i(TAG, "Checkout Error: ${errorInfo.reason}")
    }

    override fun onPayPalCancel() {
        Log.i(TAG, "User cancelled")
    }

    override fun onPayPalShippingAddressChange(
        shippingChangeData: ShippingChangeData,
        shippingChangeActions: ShippingChangeActions
    ) {
        Log.i(TAG, "ShippingAddressChange: $shippingChangeData")
    }
}
