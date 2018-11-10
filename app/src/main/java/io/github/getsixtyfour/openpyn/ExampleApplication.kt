package io.github.getsixtyfour.openpyn

import android.app.Application
import com.squareup.leakcanary.LeakCanary

open class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        installLeakCanary()
    }

    protected open fun installLeakCanary() {
        // no-op, LeakCanary is disabled in production.
    }
}
