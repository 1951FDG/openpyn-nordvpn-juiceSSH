package io.github.getsixtyfour.openpyn

import com.github.moduth.blockcanary.BlockCanary

@Suppress("unused")
class DebugMainApplication : MainApplication() {

    override fun onCreate() {
        super.onCreate()

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
