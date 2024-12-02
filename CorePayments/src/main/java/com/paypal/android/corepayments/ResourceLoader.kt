package com.paypal.android.corepayments

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RawRes
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Convenience class to simplify interaction with Android resource APIs.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ResourceLoader {

    /**
     * Load an Android raw resource as a String using a background IO thread.
     *
     * @param context Android context
     * @param resId ID of the resource that will be loaded
     */
    suspend fun loadRawResource(context: Context, @RawRes resId: Int): LoadRawResourceResult =
        withContext(Dispatchers.IO) {
            try {
                val resInputStream = context.resources.openRawResource(resId)
                val resAsBytes = ByteArray(resInputStream.available())
                resInputStream.read(resAsBytes)
                resInputStream.close()
                LoadRawResourceResult.Success(String(resAsBytes))
            } catch (e: Resources.NotFoundException) {
                val errorDescription = "Resource with id $resId not found."
                val resourceNotFoundError = PayPalSDKError(0, errorDescription, reason = e)
                LoadRawResourceResult.Failure(resourceNotFoundError)
            } catch (e: IOException) {
                val errorDescription = "Error loading resource with id $resId."
                val ioError = PayPalSDKError(0, errorDescription, reason = e)
                LoadRawResourceResult.Failure(ioError)
            }
        }
}
