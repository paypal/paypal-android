package com.paypal.android.ui.approveorderprogress

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ApproveOrderProgressViewModel: ViewModel() {

    private val _events = MutableStateFlow(listOf<String>())
    val events = _events.asStateFlow()

    fun publishEvent(message: String) {
        _events.update {
            val listCopy = it.toMutableList()
            listCopy.add(message)
            listCopy
        }
    }
}