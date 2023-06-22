package com.paypal.android.ui.approveorderprogress

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ApproveOrderProgressViewModel : ViewModel() {

    private val _eventLog = MutableStateFlow(listOf<ApproveOrderEvent>())
    val eventLog = _eventLog.asStateFlow()

    fun appendEventToLog(event: ApproveOrderEvent) {
        _eventLog.update {
            val listCopy = it.toMutableList()
            listCopy.add(event)
            listCopy
        }
    }
}