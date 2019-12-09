package io.github.getsixtyfour.openpyn

import android.os.Build
import androidx.preference.PreferenceManager
import com.github.moduth.blockcanary.BlockCanary

@Suppress("unused")
class DebugMainApplication : MainApplication() {

    override fun onCreate() {
        super.onCreate()
        // todo look for 10.0.2.2 on debug machine
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            applicationContext.classLoader.loadClass("android.os.SystemProperties").let {
                if ((it.getMethod("get", String::class.java).invoke(it, "ro.kernel.qemu") as String) == "1") {
                    // Edit Settings only for Emulator
                    PreferenceManager.getDefaultSharedPreferences(this).edit().apply {
                        // Android Emulator - Special alias to your host loopback interface (i.e., 127.0.0.1 on your development machine)
                        putString(getString(R.string.pref_openvpnmgmt_host_key), "10.0.2.2")
                        // Default management port used by Openpyn
                        putString(getString(R.string.pref_openvpnmgmt_port_key), "7015")
                        apply()
                    }
                }
            }
        }
    }

    override fun installBlockCanary() {
        BlockCanary.install(this, AppBlockCanaryContext()).start()
    }
    /*override fun installLeakCanary() {
        val sdkInt = Build.VERSION.SDK_INT
        if ((Build.VERSION_CODES.O..Build.VERSION_CODES.Q).contains(sdkInt)) {
            Log.d(
                "Application",
                "Ignoring LeakCanary on Android $sdkInt due to an Android bug. See https://github.com/square/leakcanary/issues/1081"
            )
        } else {
            LeakCanary.install(this)
        }
    }*/
}
