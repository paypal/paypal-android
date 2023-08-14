package com.paypal.android

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DemoViewModel: ViewModel() {
    private val _newIntent = MutableLiveData<Intent>()
    val newIntent: LiveData<Intent> = _newIntent

    fun setNewIntent(newIntent: Intent?) {
        newIntent?.let { _newIntent.value = it }
    }
}