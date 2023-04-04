package com.paypal.android.corepayments

import android.content.Context
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResourceLoader(context: Context) {

    private val applicationContext = context.applicationContext

    suspend fun loadRawResource(@RawRes resId: Int): String = withContext(Dispatchers.IO) {
        try {
            val resInputStream = applicationContext.resources.openRawResource(resId)
            val resAsBytes = ByteArray(resInputStream.available())
            resInputStream.read(resAsBytes)
            String(resAsBytes)
        } catch (e: Exception) {
            throw Exception("TODO: throw SDK typed error", e)
        }
    }
}