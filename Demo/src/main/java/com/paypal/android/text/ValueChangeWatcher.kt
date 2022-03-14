package com.paypal.android.text

import android.text.Editable
import android.text.TextWatcher

open class ValueChangeWatcher(private val onValueChange: ((oldValue: String, newValue: String) -> Unit)) :
    TextWatcher {

    private var oldValue: String = ""

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // no-op
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // no-op
    }

    override fun afterTextChanged(p0: Editable?) {
        if (p0 != null) {
            val newValue = p0.toString()
            if (oldValue != newValue) {
                val old = oldValue
                this.oldValue = newValue
                onValueChange(old, newValue)
            }
        }
    }
}
