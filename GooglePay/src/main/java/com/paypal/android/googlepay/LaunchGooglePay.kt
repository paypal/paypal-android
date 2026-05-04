package com.paypal.android.googlepay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts
import org.json.JSONObject

// Wrapper for Google Pay GetPaymentDataResult ActivityResultAPI contract
class LaunchGooglePay internal constructor(
    private val googlePayContract: TaskResultContracts.GetPaymentDataResult
) : ActivityResultContract<GooglePayAuthChallenge, GooglePayLaunchResult>() {

    // NOTE: this constructor is necessary to avoid having to mark play-services as
    // `api` in the :GooglePay build.gradle
    constructor(): this(TaskResultContracts.GetPaymentDataResult())

    override fun createIntent(
        context: Context,
        input: GooglePayAuthChallenge
    ): Intent {
        return googlePayContract.createIntent(context, input.task)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): GooglePayLaunchResult? {
        val googlePayResult = googlePayContract.parseResult(resultCode, intent)
        val success = googlePayResult.status.statusCode == CommonStatusCodes.SUCCESS
        val paymentMethodData = googlePayResult.result?.toJson()?.let { jsonString ->
            val json = JSONObject(jsonString)
            json.getJSONObject("paymentMethodData").toString()
        }
        return GooglePayLaunchResult(success, paymentMethodData)
    }

    override fun getSynchronousResult(
        context: Context,
        input: GooglePayAuthChallenge?
    ): SynchronousResult<GooglePayLaunchResult?>? {
        val syncResult = input?.task?.let { googlePayContract.getSynchronousResult(context, it) }
        return syncResult?.value?.let { googlePayResult ->
            val success = googlePayResult.status.statusCode == CommonStatusCodes.SUCCESS
            val paymentMethodData = googlePayResult.result?.toJson()
            return SynchronousResult(GooglePayLaunchResult(success, paymentMethodData))
        }
    }
}
