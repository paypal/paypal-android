package com.paypal.android.corepayments

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Convenience class to simplify interaction with Android resource APIs.
 */
internal class ResourceLoader(context: Context) {

    private val applicationContext = context.applicationContext

    /**
     * Load an Android raw resource as a String using a background IO thread.
     *
     * @param resId ID of the resource that will be loaded
     */
    suspend fun loadRawResource(@RawRes resId: Int): String = withContext(Dispatchers.IO) {
        try {
            val resInputStream = applicationContext.resources.openRawResource(resId)
            val resAsBytes = ByteArray(resInputStream.available())
            resInputStream.read(resAsBytes)
            resInputStream.close()
            String(resAsBytes)
        } catch (e: Resources.NotFoundException) {
            val errorDescription = "Resource with id $resId not found."
            throw PayPalSDKError(0, errorDescription, reason = e)
        } catch (e: IOException) {
            val errorDescription = "Error loading resource with id $resId."
            throw PayPalSDKError(0, errorDescription, reason = e)
        }
    }
}
