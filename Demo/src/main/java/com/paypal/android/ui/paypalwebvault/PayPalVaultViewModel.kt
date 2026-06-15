package com.paypal.android.ui.paypalwebvault

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.DemoConstants
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.CreateSetupTokenHandler
import com.paypal.android.paypalwebpayments.CreateSetupTokenResponse
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreatePayPalPaymentTokenUseCase
import com.paypal.android.usecase.CreatePayPalSetupTokenUseCase
import com.paypal.android.utils.ReturnUrlFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalVaultViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
) : ViewModel() {
    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId, SDKSampleServerAPI.merchantId)
    private val paypalClient = PayPalWebCheckoutClient(applicationContext, coreConfig)

    private val _uiState = MutableStateFlow(PayPalVaultUiState())
    val uiState = _uiState.asStateFlow()

    // Internal tracking only — not exposed in UiState since token creation is
    // an implementation detail of vaultSetupToken().
    private var createSetupTokenState: ActionState<PayPalSetupToken, Exception> = ActionState.Idle

    private val createdSetupToken: PayPalSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    private var vaultPayPalState
        get() = _uiState.value.vaultPayPalState
        set(value) {
            _uiState.update { it.copy(vaultPayPalState = value) }
        }

    private var createPaymentTokenState
        get() = _uiState.value.createPaymentTokenState
        set(value) {
            _uiState.update { it.copy(createPaymentTokenState = value) }
        }

    var returnToAppStrategy: ReturnToAppStrategyOption
        get() = _uiState.value.returnToAppStrategy
        set(value) {
            _uiState.update { it.copy(returnToAppStrategy = value) }
        }

    fun vaultSetupToken(activity: ComponentActivity) {
        vaultPayPalState = ActionState.Loading

        val strategy = returnToAppStrategy.toReturnToAppStrategy()
        val request = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = ReturnToAppUrlConfig(
                returnAppUrl = ReturnUrlFactory.createVaultSuccessUrl(strategy),
                cancelAppUrl = ReturnUrlFactory.createVaultCancelUrl(strategy),
                fallbackSchemeUrl = "${DemoConstants.APP_CUSTOM_URL_SCHEME}://paypal-sdk/paypal-vault",
            )
        )

        paypalClient.vault(activity, request, CreateSetupTokenHandler { callback ->
            viewModelScope.launch {
                when (val result = createPayPalSetupTokenUseCase(strategy)) {
                    is SDKSampleServerResult.Success -> {
                        createSetupTokenState = ActionState.Success(result.value)
                        callback(CreateSetupTokenResponse.Success(result.value.id))
                    }
                    is SDKSampleServerResult.Failure ->
                        callback(CreateSetupTokenResponse.Failure(result.value))
                }
            }
        }) { result ->
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    // Browser switch launched; wait for user to complete PayPal vault
                }
                is PayPalPresentAuthChallengeResult.Failure ->
                    vaultPayPalState = ActionState.Failure(result.error)
            }
        }
    }

    fun createPaymentToken() {
        val setupToken = createdSetupToken
        if (setupToken == null) {
            createPaymentTokenState =
                ActionState.Failure(Exception("Start vault to continue."))
        } else {
            createPaymentTokenState = ActionState.Loading
            viewModelScope.launch {
                createPaymentTokenState =
                    createPayPalPaymentTokenUseCase(setupToken).mapToActionState()
            }
        }
    }

    fun completeAuthChallenge(intent: Intent) {
        paypalClient.finishVault(intent)?.let { result ->
            vaultPayPalState = when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> ActionState.Success(result)
                is PayPalWebCheckoutFinishVaultResult.Failure -> ActionState.Failure(result.error)
                PayPalWebCheckoutFinishVaultResult.Canceled ->
                    ActionState.Failure(Exception("USER CANCELED"))
                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // no result; re-enable PayPal button so user can retry
                    ActionState.Idle
                }
            }
        }
    }
}
