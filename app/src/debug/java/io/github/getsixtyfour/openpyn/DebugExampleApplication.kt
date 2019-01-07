package io.github.getsixtyfour.openpyn

import com.squareup.leakcanary.LeakCanary
import com.github.moduth.blockcanary.BlockCanary

class DebugExampleApplication : ExampleApplication() {
    override fun installBlockCanary() {
        BlockCanary.install(this, AppBlockCanaryContext()).start()
    }

    override fun installLeakCanary() {
        LeakCanary.install(this)
    }
}
