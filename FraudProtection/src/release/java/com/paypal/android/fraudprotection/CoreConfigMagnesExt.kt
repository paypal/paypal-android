package com.paypal.android.fraudprotection

import com.paypal.android.corepayments.CoreConfig
import lib.android.paypal.com.magnessdk.Environment

internal val CoreConfig.magnesEnvironment: Environment
    get() = when (environment) {
        com.paypal.android.corepayments.Environment.LIVE -> Environment.LIVE
        com.paypal.android.corepayments.Environment.SANDBOX -> Environment.SANDBOX
    }
