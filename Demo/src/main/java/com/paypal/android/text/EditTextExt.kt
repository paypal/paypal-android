package com.paypal.android.text

import android.widget.EditText

var EditText.onValueChange: ((oldValue: String, newValue: String) -> Unit)?
    get() = null
    set(callback) {
        callback?.let { addTextChangedListener(ValueChangeWatcher(it)) }
    }
