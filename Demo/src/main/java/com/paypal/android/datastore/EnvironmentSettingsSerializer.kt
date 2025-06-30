package com.paypal.android.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// Ref: https://developer.android.com/codelabs/android-proto-datastore#5
object EnvironmentSettingsSerializer : Serializer<EnvironmentSettings> {
    private val defaultSandboxEnvironment = DemoEnvironment
        .newBuilder()
        .setName("PPCP Mobile US – Sandbox")
        .setPayPalEnvironment(PayPalEnvironment.SANDBOX)
        .setServerUrl("https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com/")
        .setClientId("AQTfw2irFfemo-eWG4H5UY-b9auKihUpXQ2Engl4G1EsHJe2mkpfUv_SN3Mba0v3CfrL6Fk_ecwv9EOo")
        .build()

    override val defaultValue: EnvironmentSettings = EnvironmentSettings
        .newBuilder()
        .addEnvironments(0, defaultSandboxEnvironment)
        .setActiveEnvironmentIndex(0)
        .build()

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

val Context.environmentSettingsDataStore: DataStore<EnvironmentSettings> by dataStore(
    fileName = "environment_settings.pb",
    serializer = EnvironmentSettingsSerializer
)