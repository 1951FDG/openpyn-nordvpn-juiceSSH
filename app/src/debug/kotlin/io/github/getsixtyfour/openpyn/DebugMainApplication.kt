package io.github.getsixtyfour.openpyn

import android.os.Build
import android.util.Log
import com.github.moduth.blockcanary.BlockCanary
import com.squareup.leakcanary.LeakCanary

@Suppress("unused")
class DebugMainApplication : MainApplication() {

    override fun installBlockCanary() {
        val sdkInt = Build.VERSION.SDK_INT
        if ((Build.VERSION_CODES.O..Build.VERSION_CODES.P).contains(sdkInt)) {
            Log.d(
                "Application",
                "Ignoring LeakCanary on Android $sdkInt due to an Android bug. See https://github.com/square/leakcanary/issues/1081"
            )
        } else {
            BlockCanary.install(this, AppBlockCanaryContext()).start()
        }
    }

    override fun installLeakCanary() {
        LeakCanary.install(this)
    }
}
