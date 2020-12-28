package io.github.getsixtyfour.openpyn

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.preference.PreferenceManager
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun printAPIValues(context: Context) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val ipdata = preferences.getString("pref_api_ipdata", "")!!
    val ipinfo = preferences.getString("pref_api_ipinfo", "")!!
    val ipstack = preferences.getString("pref_api_ipstack", "")!!
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    logger.error("ipdata = $ipdata")
    logger.error("ipinfo = $ipinfo")
    logger.error("ipstack = $ipstack")
    logger.error("$manufacturer $model")
}

fun isRunningTest(): Boolean = try {
    Class.forName("androidx.test.espresso.Espresso")
    true
} catch (e: ClassNotFoundException) {
    false
}

fun initStrictMode() {
    if (!AppConfig.STRICT_MODE) {
        return
    }

    StrictMode.setThreadPolicy(
        ThreadPolicy.Builder().detectAll().penaltyLog().build()
    )

    StrictMode.setVmPolicy(
        VmPolicy.Builder().detectAll().penaltyLog().build()
    )
}

@SuppressLint("PrivateApi")
fun <T : Context> isEmulator(context: T): Boolean {
    // TODO: add connectivity check for 10.0.2.2 on debug machine for higher api levels
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
        context.classLoader.loadClass("android.os.SystemProperties").run {
            if ((getMethod("get", String::class.java).invoke(this, "ro.kernel.qemu") as String) == "1") {
                return@isEmulator true
            }
        }
    }
    return false
}

fun <T : Context> saveEmulatorPreferences(context: T) {
    if (!AppConfig.EMULATOR) {
        return
    }

    PreferenceManager.getDefaultSharedPreferences(context).edit().run {
        // Android Emulator - Special alias to your host loopback interface (i.e., 127.0.0.1 on your development machine)
        putString(context.getString(R.string.pref_openvpnmgmt_host_key), "10.0.2.2")
        // The default port for Telnet client connections is 23
        putString(context.getString(R.string.pref_openvpnmgmt_port_key), "23")
    }.apply()
}
