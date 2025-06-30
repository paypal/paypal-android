package com.paypal.android.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object EnvironmentSettingsSerializer : Serializer<EnvironmentSettings> {
    override val defaultValue: EnvironmentSettings = EnvironmentSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): EnvironmentSettings {
        try {
            return EnvironmentSettings.parseFrom(input)
        } catch (exception: IOException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: EnvironmentSettings,
        output: OutputStream
    ) = t.writeTo(output)
}

val Context.settingsDataStore: DataStore<EnvironmentSettings> by dataStore(
    fileName = "environment_settings.pb",
    serializer = EnvironmentSettingsSerializer
)