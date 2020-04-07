package io.github.getsixtyfour.openpyn

import android.app.Application
import io.github.getsixtyfour.openpyn.utils.NetworkInfo

// TODO: System.setProperty("kotlinx.coroutines.fast.service.loader", "false")
open class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        setDefaultPreferences(this)

        NetworkInfo.getInstance(this)

        populateAboutConfig()

        installBlockCanary()
    }

    protected open fun installBlockCanary() {
        // No-op, BlockCanary is disabled in production
    }
}
