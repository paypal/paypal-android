package com.paypal.android

import android.app.Application
import com.paypal.android.di.apiModule
import com.paypal.android.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@DemoApplication)
            modules(moduleList)
        }
    }

    companion object {
        private val moduleList = listOf(
            apiModule,
            networkModule
        )
    }
}
