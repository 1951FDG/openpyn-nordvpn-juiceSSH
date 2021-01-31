package io.github.getsixtyfour.openpyn

@Suppress("unused")
class DebugMainApplication : MainApplication() {

    override fun onCreate() {
        super.onCreate()

        initStrictMode()
        initBlockCanary(this)
        initEmulatorPreferences(this)
        initStetho(this)
        initTimber(this)
    }
}
