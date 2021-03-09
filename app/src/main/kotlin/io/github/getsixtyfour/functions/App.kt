package io.github.getsixtyfour.functions

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import com.eggheadgames.aboutbox.AboutConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.smartdengg.clickdebounce.DebouncedPredictor
import io.github.getsixtyfour.openpyn.BuildConfig
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import timber.log.CrashReportingTree
import timber.log.Timber

@SuppressLint("DefaultLocale")
fun initAboutConfig(context: Context) {
    val config = AboutConfig.getInstance()
    // General info
    config.appName = context.getString(R.string.app_name)
    config.appIcon = R.mipmap.ic_launcher
    config.version = context.getString(R.string.app_version)
    config.buildType = AboutConfig.BuildType.GOOGLE
    config.packageName = context.getString(R.string.app_id)
    // Email
    config.emailAddress = context.getString(R.string.email_address)
    config.emailSubject = context.getString(R.string.empty)
    config.emailBody = context.getString(R.string.empty)
    config.emailBodyPrompt = context.getString(R.string.empty)
    // Share
    config.shareMessage = context.getString(R.string.empty)
    config.sharingTitle = context.getString(R.string.share_message)
}

fun initPreferences(context: Context) {
    PreferenceManager.setDefaultValues(context, R.xml.pref_settings, false)
    PreferenceManager.setDefaultValues(context, R.xml.pref_api, true)
    PreferenceManager.setDefaultValues(context, R.xml.pref_openvpnmgmt, true)
    PreferenceManager.setDefaultValues(context, R.xml.pref_connect, true)
}

fun initCrashlytics(context: Context) {
    if (BuildConfig.DEBUG) {
        return
    }
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val telemetry = preferences.getBoolean("pref_telemetry", true)

    if (telemetry) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        Timber.plant(CrashReportingTree())
    }
}

fun initNetworkInfo(context: Context) {
    NetworkInfo.getInstance(context)
}

@Suppress("MagicNumber")
fun initClickDebounce() {
    DebouncedPredictor.FROZEN_WINDOW_MILLIS = 500L
}
