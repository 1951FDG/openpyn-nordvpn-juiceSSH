package io.github.getsixtyfour.openpyn

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.michaelflisar.gdprdialog.GDPR
import com.squareup.leakcanary.LeakCanary
import io.github.getsixtyfour.openpyn.utils.NetworkInfo

open class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // System.setProperty("kotlinx.coroutines.fast.service.loader", "false")
        MapsInitializer.initialize(this) //todo check value
        setDefaultPreferences(this)

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        NetworkInfo.getInstance(this)
        val sdkInt = Build.VERSION.SDK_INT
        if ((Build.VERSION_CODES.O..Build.VERSION_CODES.P).contains(sdkInt)) {
            Log.d(
                "Application",
                "Ignoring LeakCanary on Android $sdkInt due to an Android bug. See https://github.com/square/leakcanary/issues/1081"
            )
        } else {
            installLeakCanary()
        }

        installBlockCanary()
        populateAboutConfig()
        GDPR.getInstance().init(this)
    }

    protected open fun installBlockCanary() {
        // no-op, BlockCanary is disabled in production.
    }

    protected open fun installLeakCanary() {
        // no-op, LeakCanary is disabled in production.
    }
}
