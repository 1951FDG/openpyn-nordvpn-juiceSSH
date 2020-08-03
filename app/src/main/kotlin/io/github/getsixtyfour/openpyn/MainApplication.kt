package io.github.getsixtyfour.openpyn

import android.app.Application
import io.github.getsixtyfour.openpyn.utils.NetworkInfo

open class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initCrashlytics(this, initGDPR(this).consentState)

        initTimber()

        setDefaultPreferences(this)

        NetworkInfo.getInstance(this)

        populateAboutConfig()

        installBlockCanary()
    }

    protected open fun installBlockCanary() {
        // No-op, BlockCanary is disabled in production
    }
}
