package com.paypal.android.corepayments

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ResourceLoader(context: Context) {

    private val applicationContext = context.applicationContext

    suspend fun loadRawResource(@RawRes resId: Int): String = withContext(Dispatchers.IO) {
        try {
            val resInputStream = applicationContext.resources.openRawResource(resId)
            val resAsBytes = ByteArray(resInputStream.available())
            resInputStream.read(resAsBytes)
            resInputStream.close()
            String(resAsBytes)
        } catch (e: Resources.NotFoundException) {
            // TODO: come up with better error messages
            throw PayPalSDKError(0, "Resource not found.", reason = e)
        } catch (e: IOException) {
            // TODO: come up with better error messages
            throw PayPalSDKError(0, "Error loading raw resource.", reason = e)
        }
    }
}
