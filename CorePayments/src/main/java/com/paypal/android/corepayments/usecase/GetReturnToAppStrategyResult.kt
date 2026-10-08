package com.paypal.android.corepayments.usecase

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.ReturnToAppStrategy

/**
 * Result of [GetReturnToAppStrategyUseCase].
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class GetReturnToAppStrategyResult {

    /**
     * A [returnToAppStrategy] was resolved.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Success(val returnToAppStrategy: ReturnToAppStrategy) : GetReturnToAppStrategyResult()

    /**
     * No usable return route back to the merchant app could be resolved.
     *
     * @property error Human-readable description of why a strategy couldn't be resolved.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failure(val error: String) : GetReturnToAppStrategyResult()
}
