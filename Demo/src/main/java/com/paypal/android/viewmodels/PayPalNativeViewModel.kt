package com.paypal.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paypal.android.usecase.GetAccessTokenUseCase
import com.paypal.android.usecase.GetApprovalSessionIdActionUseCase
import com.paypal.android.usecase.GetBillingAgreementTokenUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.android.usecase.GetOrderUseCase
import com.paypal.android.PayPalConfigConstants
import com.paypal.android.checkout.PayPalCheckoutError
import com.paypal.android.checkout.PayPalCheckoutListener
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.ErrorInfo
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    private val getBillingAgreementTokenUseCase: GetBillingAgreementTokenUseCase,
    private val getAccessTokenUseCase: GetAccessTokenUseCase,
    private val getOrderIdUseCase: GetOrderIdUseCase,
    private val getApprovalSessionIdActionUseCase: GetApprovalSessionIdActionUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val getOrderUseCase: GetOrderUseCase = GetOrderUseCase()

    private val payPalConstants = PayPalConfigConstants()

    private val payPalListener = object : PayPalCheckoutListener {
        override fun onPayPalCheckoutStart() {
            internalState.postValue(ViewState.CheckoutStart)
        }

        override fun onPayPalCheckoutSuccess(result: PayPalCheckoutResult) {
            result.approval.data.apply {
                internalState.postValue(ViewState.CheckoutComplete(
                    payerId,
                    orderId,
                    paymentId,
                    billingToken
                ))
            }
        }

        override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
            val errorState = when (error) {
                is PayPalCheckoutError -> ViewState.CheckoutError(error = error.errorInfo)
                else -> ViewState.CheckoutError(message = error.errorDescription)
            }
            internalState.postValue(errorState)
        }

        override fun onPayPalCheckoutCanceled() {
            internalState.postValue(ViewState.CheckoutCancelled)
        }

        override fun onPayPalCheckoutShippingChange(
            shippingChangeData: ShippingChangeData,
            shippingChangeActions: ShippingChangeActions
        ) {
            // implement
        }
    }

    private val internalState = MutableLiveData<ViewState>(ViewState.Initial)
    val state: LiveData<ViewState> = internalState

    lateinit var payPalClient: PayPalClient

    fun billingAgreementCheckout() {
        viewModelScope.launch {
            val order = getBillingAgreementTokenUseCase()
            order.id?.also { orderId ->
                startCheckoutFlow(CreateOrder { createOrderActions ->
                    createOrderActions.setBillingAgreementId(orderId)
                })
            }
        }
    }

    fun fetchAccessToken() {
        internalState.postValue(ViewState.GeneratingToken)
        viewModelScope.launch {
            try {
                val accessToken = getAccessTokenUseCase()
                initPayPalClient(accessToken)
                internalState.postValue(ViewState.TokenGenerated(accessToken))
            } catch (e: HttpException) {
                internalState.postValue(ViewState.CheckoutError(message = e.message))
            }
        }
    }

    fun orderCheckout() {
        val order = getOrderUseCase()
        startCheckoutFlow(CreateOrder { createOrderActions ->
            createOrderActions.create(order) {
                internalState.postValue(ViewState.OrderCreated(it))
            }
        })
    }

    fun orderIdCheckout() {
        viewModelScope.launch {
            val orderId = getOrderIdUseCase()
            orderId?.also {
                startCheckoutFlow(CreateOrder { createOrderActions ->
                    createOrderActions.set(it)
                    internalState.postValue(ViewState.OrderCreated(it))
                })
            }
        }
    }

    fun vaultCheckout() {
        viewModelScope.launch {
            val sessionId = getApprovalSessionIdActionUseCase()
            sessionId?.also {
                startCheckoutFlow(CreateOrder { createOrderActions ->
                    createOrderActions.setVaultApprovalSessionId(it)
                })
            }
        }
    }

    private fun startCheckoutFlow(createOrder: CreateOrder) {
        payPalClient.startCheckout(createOrder)
    }

    private fun initPayPalClient(accessToken: String) {
        payPalClient = PayPalClient(
            getApplication(),
            CoreConfig(accessToken),
            payPalConstants.returnUrl
        )
        payPalClient.listener = payPalListener
    }

    sealed class ViewState {
        object Initial : ViewState()
        object BillingAgreementState : ViewState()
        object VaultV2State : ViewState()
        object GeneratingToken : ViewState()
        object ErrorGeneratingToken : ViewState()
        class TokenGenerated(val token: String) : ViewState()
        class OrderCreated(val orderId: String) : ViewState()
        object CheckoutStart : ViewState()
        object CheckoutCancelled : ViewState()
        class CheckoutError(val message: String? = null, val error: ErrorInfo? = null) : ViewState()
        data class CheckoutComplete(
            val payerId: String?,
            val orderId: String?,
            val paymentId: String?,
            val billingToken: String? = null,
        ) : ViewState()
    }
}
