package com.paypal.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.GetAccessTokenUseCase
import com.paypal.android.GetApprovalSessionIdActionUseCase
import com.paypal.android.GetBillingAgreementTokenUseCase
import com.paypal.android.GetOrderIdUseCase
import com.paypal.android.GetOrderUseCase
import com.paypal.android.PayPalConfigConstants
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.core.CoreConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.OrderIntent
import com.paypal.checkout.order.Amount
import com.paypal.checkout.order.AppContext
import com.paypal.checkout.order.Order
import com.paypal.checkout.order.PurchaseUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    private val getBillingAgreementTokenUseCase: GetBillingAgreementTokenUseCase,
    private val getAccessTokenUseCase: GetAccessTokenUseCase,
    private val getOrderUseCase: GetOrderUseCase = GetOrderUseCase(),
    private val getOrderIdUseCase: GetOrderIdUseCase,
    private val getApprovalSessionIdActionUseCase: GetApprovalSessionIdActionUseCase,
    application: Application
): AndroidViewModel(application) {

    private val payPalConstants = PayPalConfigConstants()

    lateinit var payPalClient: PayPalClient

    fun billingAgreementCheckout() {
        viewModelScope.launch {
            val order = getBillingAgreementTokenUseCase()
            order.id?.also { orderId ->
                payPalClient.startCheckout(CreateOrder { createOrderActions ->
                    createOrderActions.setBillingAgreementId(orderId)
                })
            }
        }
    }

    fun fetchAccessToken() {
        viewModelScope.launch {
            try {
                val accessToken = getAccessTokenUseCase()
                initPayPalClient(accessToken)
            } catch (e: Exception) {
                //emit error state
            }
        }
    }

    fun orderCheckout() {
        val order = getOrderUseCase()
        payPalClient.startCheckout(CreateOrder { createOrderActions ->
            createOrderActions.create(order) {
                //log order id
            }
        })
    }

    fun orderIdCheckout() {
        viewModelScope.launch {
            val orderId = getOrderIdUseCase()
            orderId?.also {
                payPalClient.startCheckout(CreateOrder { createOrderActions ->
                    createOrderActions.set(it)
                })
            }
        }
    }

    fun vaultCheckout() {
        viewModelScope.launch {
            val sessionId = getApprovalSessionIdActionUseCase()
            sessionId?.also {
                payPalClient.startCheckout(CreateOrder { createOrderActions ->
                    createOrderActions.setVaultApprovalSessionId(it)
                })
            }
        }
    }

    private fun initPayPalClient(accessToken: String) {
        payPalClient = PayPalClient(
            getApplication(),
            CoreConfig(accessToken),
            payPalConstants.returnUrl
        )
    }

    companion object {

    }
}