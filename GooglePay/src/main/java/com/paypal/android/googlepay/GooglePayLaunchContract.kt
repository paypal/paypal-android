package com.paypal.android.googlepay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts

// Wrapper for Google Pay launch
class GooglePayLaunchContract(
    private val googlePayContract: TaskResultContracts.GetPaymentDataResult = TaskResultContracts.GetPaymentDataResult()
) : ActivityResultContract<GooglePayAuthChallenge, GooglePayLaunchResult>() {

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
        return GooglePayLaunchResult(success)
    }

    override fun getSynchronousResult(
        context: Context,
        input: GooglePayAuthChallenge?
    ): SynchronousResult<GooglePayLaunchResult?>? {
        val result = input?.task?.let { googlePayContract.getSynchronousResult(context, it) }
        return result?.value?.let { value ->
            val success = value.status.statusCode == CommonStatusCodes.SUCCESS
            return SynchronousResult(GooglePayLaunchResult(success))
        }
    }
}
