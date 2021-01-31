package io.github.getsixtyfour.openpyn

import android.app.Application

open class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initAboutConfig()
        initPreferences(this)
        initCrashlytics(this)
        initNetworkInfo(this)
    }
}
