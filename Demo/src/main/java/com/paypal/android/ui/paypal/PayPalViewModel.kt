package com.paypal.android.ui.paypal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.paypal.android.checkout.PayPalListener

class PayPalViewModel : ViewModel() {
    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
}
