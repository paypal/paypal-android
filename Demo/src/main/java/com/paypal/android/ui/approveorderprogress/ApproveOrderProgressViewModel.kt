package com.paypal.android.ui.approveorderprogress

import androidx.lifecycle.ViewModel
import com.paypal.android.uishared.events.ComposableEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ApproveOrderProgressViewModel : ViewModel() {

    private val _eventLog = MutableStateFlow(listOf<ComposableEvent>())
    val eventLog = _eventLog.asStateFlow()

    fun appendEventToLog(event: ComposableEvent) {
        _eventLog.update {
            val listCopy = it.toMutableList()
            listCopy.add(event)
            listCopy
        }
    }
}