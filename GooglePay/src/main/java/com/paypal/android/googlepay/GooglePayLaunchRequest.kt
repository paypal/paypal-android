package com.paypal.android.googlepay

import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData

data class GooglePayLaunchRequest(internal val task: Task<PaymentData>)