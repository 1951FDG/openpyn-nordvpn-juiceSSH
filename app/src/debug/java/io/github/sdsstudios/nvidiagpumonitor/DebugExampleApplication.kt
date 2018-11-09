package io.github.sdsstudios.nvidiagpumonitor

import com.squareup.leakcanary.LeakCanary

class DebugExampleApplication : ExampleApplication() {
    override fun installLeakCanary() {
        LeakCanary.install(this)
    }
}
