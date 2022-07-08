package com.paypal.android.ui.paypal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.api.EligibilityAPI
import com.paypal.android.core.api.models.Eligibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalViewModel
@Inject constructor(
    private val eligibilityAPI: EligibilityAPI
) : ViewModel() {

    fun getEligibility(): LiveData<Eligibility> {
        val liveData: MutableLiveData<Eligibility> = MutableLiveData()
        viewModelScope.launch {
            try {
                liveData.postValue(eligibilityAPI.checkEligibility())
            } catch (error: PayPalSDKError) {
                Log.d(TAG, "Error: ${error.message}")
            }
        }
        return liveData
    }

    companion object {
        const val TAG = "PayPalViewModelTag"
    }
}
