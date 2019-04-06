package io.github.getsixtyfour.openpyn

import android.app.Application
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.IAnalytic
import com.michaelflisar.gdprdialog.GDPR
import com.squareup.leakcanary.LeakCanary
import io.github.getsixtyfour.openpyn.utilities.logException

open class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        installBlockCanary()
        installLeakCanary()
        populateAboutConfig()
        GDPR.getInstance().init(this)
    }

    protected open fun installBlockCanary() {
        // no-op, BlockCanary is disabled in production.
    }

    protected open fun installLeakCanary() {
        // no-op, LeakCanary is disabled in production.
    }

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
}
