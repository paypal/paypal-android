package com.paypal.android.ui.paypal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.core.api.EligibilityAPI
import com.paypal.android.core.api.models.APIResult
import com.paypal.android.core.api.models.Eligibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalViewModel
@Inject constructor(
    private val eligibilityAPI: EligibilityAPI
) : ViewModel() {

    fun getEligibility(): LiveData<APIResult<Eligibility>> {
        val liveData = MutableLiveData<APIResult<Eligibility>>()
        viewModelScope.launch {
            liveData.postValue(eligibilityAPI.checkEligibility())
        }
        return liveData
    }
}
