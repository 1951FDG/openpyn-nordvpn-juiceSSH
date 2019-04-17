package io.github.getsixtyfour.openpyn

import com.github.moduth.blockcanary.BlockCanary
import com.squareup.leakcanary.LeakCanary

class DebugMainApplication : MainApplication() {
    override fun installBlockCanary() {
        BlockCanary.install(this, AppBlockCanaryContext()).start()
    }

    override fun installLeakCanary() {
        LeakCanary.install(this)
    }
}
