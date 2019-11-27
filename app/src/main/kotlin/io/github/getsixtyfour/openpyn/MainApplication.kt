package io.github.getsixtyfour.openpyn

import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import com.michaelflisar.gdprdialog.GDPR
import io.github.getsixtyfour.openpyn.utils.NetworkInfo

open class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /*if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }*/

        MapsInitializer.initialize(this) //todo check value
        setDefaultPreferences(this)

        NetworkInfo.getInstance(this)

        populateAboutConfig()

        if (!isRunningTest()) {
            // installLeakCanary()
            installBlockCanary()
            GDPR.getInstance().init(this)
        }
        // System.setProperty("kotlinx.coroutines.fast.service.loader", "false")
    }

    protected open fun installBlockCanary() {
        // no-op, BlockCanary is disabled in production.
    }

    protected open fun installLeakCanary() {
        // no-op, LeakCanary is disabled in production.
    }
}
