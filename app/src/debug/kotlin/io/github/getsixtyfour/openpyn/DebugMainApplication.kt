package io.github.getsixtyfour.openpyn

import com.facebook.stetho.Stetho
import com.facebook.stetho.timber.StethoTree
import com.github.moduth.blockcanary.BlockCanary
import io.github.getsixtyfour.timber.DebugFileLoggingTree
import timber.log.Timber

@Suppress("unused")
class DebugMainApplication : MainApplication() {

    override fun onCreate() {
        super.onCreate()

        Stetho.initializeWithDefaults(this)

        Timber.plant(Timber.DebugTree())
        Timber.plant(StethoTree())
        externalCacheDir?.let {
            Timber.plant(DebugFileLoggingTree(it, this))
        }

        if (isEmulator(this)) saveEmulatorPreferences(this)

        initStrictMode()
    }

    override fun installBlockCanary() {
        if (!AppConfig.UI_BLOCK_DETECTION) {
            return
        }

        if (isRunningTest()) {
            return
        }

        BlockCanary.install(this, AppBlockCanaryContext()).start()
    }
}
