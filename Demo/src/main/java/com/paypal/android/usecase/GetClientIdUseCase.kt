package com.paypal.android.usecase

import retrofit2.HttpException
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.APIClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import javax.inject.Inject

class GetClientIdUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {
    suspend operator fun invoke(): UseCaseResult<String, Exception> = withContext(Dispatchers.IO) {
        try {
            val clientId = sdkSampleServerAPI.fetchClientId()
            UseCaseResult.Success(clientId)
        } catch (e: UnknownHostException) {
            UseCaseResult.Failure(APIClientError.payPalCheckoutError(e.message!!))
        } catch (e: HttpException) {
            UseCaseResult.Failure(APIClientError.payPalCheckoutError(e.message!!))
        }
    }
}
