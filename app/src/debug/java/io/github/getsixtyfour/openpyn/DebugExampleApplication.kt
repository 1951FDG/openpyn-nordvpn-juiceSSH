package io.github.sdsstudios.nvidiagpumonitor

import com.squareup.leakcanary.LeakCanary
import io.github.getsixtyfour.openpyn.ExampleApplication

class DebugExampleApplication : ExampleApplication() {
    override fun installLeakCanary() {
        LeakCanary.install(this)
    }
}
