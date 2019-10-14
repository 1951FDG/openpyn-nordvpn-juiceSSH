package io.github.getsixtyfour.openpyn

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.IAnalytic
import com.google.android.gms.maps.MapsInitializer
import com.michaelflisar.gdprdialog.GDPR
import com.squareup.leakcanary.LeakCanary
import io.github.getsixtyfour.openpyn.utilities.NetworkInfo

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

    //todo inner class
    private fun populateAboutConfig() {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        val buildType = BuildConfig.BUILD_TYPE.capitalize()
        val aboutConfig = AboutConfig.getInstance()
        // general info
        aboutConfig.appName = "openpyn-nordvpn-juiceSSH"
        aboutConfig.appIcon = R.mipmap.ic_launcher
        aboutConfig.version = "$buildType $versionName ($versionCode)"
        aboutConfig.author = "1951FDG"
        aboutConfig.companyHtmlPath = "https://github.com/" + aboutConfig.author
        aboutConfig.webHomePage = aboutConfig.companyHtmlPath + '/' + aboutConfig.appName
        aboutConfig.buildType = AboutConfig.BuildType.GOOGLE
        aboutConfig.packageName = BuildConfig.APPLICATION_ID
        // custom analytics, dialog and share
        aboutConfig.analytics = object : IAnalytic {
            override fun logUiEvent(s: String, s1: String) {
                // handle log events.
            }

            override fun logException(e: Exception, b: Boolean) {
                // handle exception events.
                logException(e)
            }
        }
        // email
        aboutConfig.emailAddress = "support@1951fdg.com"
        aboutConfig.emailSubject = ""
        aboutConfig.emailBody = ""
        aboutConfig.emailBodyPrompt = ""
        // share
        aboutConfig.shareMessage = ""
        aboutConfig.sharingTitle = "Share"
    }

    private fun setDefaultPreferences(context: Context) {
        PreferenceManager.setDefaultValues(context, R.xml.pref_settings, false)
        PreferenceManager.setDefaultValues(context, R.xml.pref_api, true)
    }
}
