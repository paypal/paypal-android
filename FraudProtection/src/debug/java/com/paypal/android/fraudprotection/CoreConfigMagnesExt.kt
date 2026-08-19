package com.paypal.android.fraudprotection

import com.paypal.android.corepayments.CoreConfig
import lib.android.paypal.com.magnessdk.Environment

internal val CoreConfig.magnesEnvironment: Environment
    get() = when (coreEnvironment) {
        com.paypal.android.corepayments.CoreEnvironment.LIVE -> Environment.LIVE
        com.paypal.android.corepayments.CoreEnvironment.SANDBOX -> Environment.SANDBOX
        com.paypal.android.corepayments.CoreEnvironment.CUSTOM -> Environment.SANDBOX
    }
