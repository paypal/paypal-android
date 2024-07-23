package com.paypal.android.ui.venmocheckout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.features.eligibility.CheckEligibilityResultListener
import com.paypal.android.corepayments.features.eligibility.EligibilityClient
import com.paypal.android.corepayments.features.eligibility.EligibilityRequest
import com.paypal.android.corepayments.features.eligibility.EligibilityResult
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VenmoCheckoutViewModel @Inject constructor(
    private val getClientIdUseCase: GetClientIdUseCase,
) : ViewModel() {

    companion object {
        const val TAG = "VenmoViewModel"
    }

    private lateinit var eligibilityClient: EligibilityClient

    private val _uiState = MutableStateFlow(VenmoCheckoutUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    private var checkEligibilityState
        get() = _uiState.value.checkEligibilityState
        set(value) {
            _uiState.update { it.copy(checkEligibilityState = value) }
        }

    fun getEligibility(context: Context) {
        checkEligibilityState = ActionState.Loading
        viewModelScope.launch {
            when (val clientIdResult = getClientIdUseCase()) {
                is SDKSampleServerResult.Failure -> {
                    checkEligibilityState = clientIdResult.mapToActionState()
                }

                is SDKSampleServerResult.Success -> {
                    val clientId = clientIdResult.value
                    val coreConfig = CoreConfig(clientId = clientId)

                    eligibilityClient = EligibilityClient(context, coreConfig)
                    val eligibilityRequest =
                        EligibilityRequest(intent = intentOption, currencyCode = "USD")
                    eligibilityClient.check(eligibilityRequest, object : CheckEligibilityResultListener {
                        override fun onCheckEligibilitySuccess(result: EligibilityResult) {
                            checkEligibilityState = ActionState.Success(result)
                        }

                        override fun onCheckEligibilityFailure(error: PayPalSDKError) {
                            checkEligibilityState = ActionState.Failure(error)
                        }
                    })
                }
            }
        }
    }
}
