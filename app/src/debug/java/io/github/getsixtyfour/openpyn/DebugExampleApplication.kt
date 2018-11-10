package io.github.getsixtyfour.openpyn

import com.squareup.leakcanary.LeakCanary

class DebugExampleApplication : ExampleApplication() {
    override fun installLeakCanary() {
        LeakCanary.install(this)
    }
}
