package io.github.getsixtyfour.openpyn

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.eggheadgames.aboutbox.AboutConfig
import com.getsixtyfour.openvpnmgmt.android.Constants
import com.getsixtyfour.openvpnmgmt.android.Utils
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.smartdengg.clickdebounce.DebouncedPredictor
import io.github.getsixtyfour.ktextension.JUICE_SSH_PACKAGE_NAME
import io.github.getsixtyfour.ktextension.verifyInstallerId
import io.github.getsixtyfour.ktextension.verifySigningCertificate
import io.github.getsixtyfour.openpyn.map.util.createJson
import io.github.getsixtyfour.openpyn.map.util.generateXML
import io.github.getsixtyfour.openpyn.map.util.stringifyJsonArray
import io.github.getsixtyfour.openpyn.map.util.writeJsonArray
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import io.github.getsixtyfour.openpyn.utils.VpnAuthenticationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import timber.log.CrashReportingTree
import timber.log.Timber
import tk.wasdennnoch.progresstoolbar.ProgressToolbar

private val logger = KotlinLogging.logger {}

fun <T : FragmentActivity> getCurrentNavigationFragment(activity: T): Fragment? {
    val navHostFragment = activity.supportFragmentManager.primaryNavigationFragment as? NavHostFragment
    val host = navHostFragment?.host
    if (host == null) {
        logger.error(IllegalStateException()) { "Fragment $navHostFragment has not been attached yet." }
    }

    return when (host) {
        null -> null
        else -> navHostFragment.childFragmentManager.primaryNavigationFragment
    }
}

fun <T : Activity> CoroutineScope.onGenerateItemSelected(activity: T): Job = launch {
    val toolbar = (activity.findViewById(R.id.toolbar) as? ProgressToolbar)?.apply { showProgress(true) }

    withContext(Dispatchers.IO) {
        runCatching {
            generateXML()
        }
    }.onFailure {
        logger.debug(it) { "" }
    }

    toolbar?.hideProgress(true)
}

fun <T : Activity> onLicensesItemSelected(activity: T) {
    OssLicensesMenuActivity.setActivityTitle(activity.getString(R.string.title_licenses))
    val intent = Intent(activity, OssLicensesMenuActivity::class.java)
    ContextCompat.startActivity(activity, intent, null)
}

fun <T : Activity> onLoggingItemSelected(activity: T) {
    val intent = Intent().apply { component = ComponentName(activity, "info.hannes.logcat.LogfileActivity") }
    ContextCompat.startActivity(activity, intent, null)
}

fun <T : Activity> CoroutineScope.onRefreshItemSelected(activity: T): Job = launch {
    if (!NetworkInfo.getInstance().isOnline()) return@launch
    val toolbar = (activity.findViewById(R.id.toolbar) as? ProgressToolbar)?.apply { showProgress(true) }

    withContext(Dispatchers.IO) {
        runCatching {
            createJson()?.let(::stringifyJsonArray)?.let { writeJsonArray(activity, R.raw.nordvpn, it) }
        }
    }.onSuccess {
        toolbar?.hideProgress(true)
        showRefreshAlertDialog(activity)
    }.onFailure {
        toolbar?.hideProgress(true)
        logger.debug(it) { "" }
    }
}

fun <T : Activity> onSettingsItemSelected(activity: T) {
    SettingsActivity.startSettingsFragment(activity)
}

fun <T : AppCompatActivity> setProgressToolBar(
    activity: T, toolbar: ProgressToolbar, showHomeAsUp: Boolean = false, showTitle: Boolean = false
) {
    toolbar.hideProgress()
    toolbar.isIndeterminate = true

    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.setDisplayHomeAsUpEnabled(showHomeAsUp)
    activity.supportActionBar?.setDisplayShowTitleEnabled(showTitle)
}

fun <T : Activity> showRefreshAlertDialog(activity: T) {
    AlertDialog.Builder(activity).apply {
        setTitle(R.string.title_warning)
        setMessage(R.string.warning_restart_app)
        setPositiveButton(android.R.string.ok, null)
    }.show()
}

fun <T : Activity> showJuiceAlertDialog(activity: T) {
    AlertDialog.Builder(activity).apply {
        setTitle(R.string.title_error)
        setMessage(R.string.error_juicessh_server)
        setPositiveButton(android.R.string.ok) { dialog, _ ->
            context.packageManager.getLaunchIntentForPackage(JUICE_SSH_PACKAGE_NAME)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                ContextCompat.startActivity((dialog as AlertDialog).context, it, null)
            }
        }
    }.show()
}

fun <T : Activity> showOpenpynAlertDialog(activity: T) {
    AlertDialog.Builder(activity).apply {
        setTitle(R.string.title_warning)
        setMessage(R.string.warning_install_openpyn)
        setPositiveButton(android.R.string.ok) { dialog, _ ->
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/1951FDG/openpyn-nordvpn/tree/test")).let {
                ContextCompat.startActivity((dialog as AlertDialog).context, it, null)
            }
        }
    }.show()
}

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

fun <T : Activity> startVpnService(activity: T) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    val openvpnmgmt = preferences.getBoolean(activity.getString(R.string.pref_openvpnmgmt_key), false)
    if (openvpnmgmt) {
        val host = VpnAuthenticationHandler.getHost(activity)
        val port = VpnAuthenticationHandler.getPort(activity)
        val password = VpnAuthenticationHandler.getPassword(activity)
        val userName = VpnAuthenticationHandler.getUserName(activity)
        val userPass = VpnAuthenticationHandler.getUserPass(activity)
        val shouldPostByteCount = VpnAuthenticationHandler.shouldPostByteCount(activity)
        val shouldPostStateChange = VpnAuthenticationHandler.shouldPostStateChange(activity)
        val bundle = Bundle().apply {
            putBoolean(Constants.EXTRA_POST_BYTE_COUNT_NOTIFICATION, shouldPostByteCount)
            putBoolean(Constants.EXTRA_POST_STATE_NOTIFICATION, shouldPostStateChange)
            putString(Constants.EXTRA_HOST, host)
            putInt(Constants.EXTRA_PORT, port)
            putCharArray(Constants.EXTRA_PASSWORD, password)
            putString(Constants.EXTRA_VPN_USERNAME, userName)
            putString(Constants.EXTRA_VPN_PASSWORD, userPass)
        }

        Utils.doStartService(activity, bundle)
    }
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

fun getVersionTitleType(context: Context): Int = when {
    isPlayStorePackage(context) -> R.string.egab_play_store_version
    else -> R.string.egab_version
}

fun isPlayStorePackage(context: Context): Boolean = context.verifyInstallerId(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE)

fun isPlayStoreCertificate(context: Context): Boolean = context.verifySigningCertificate(listOf(context.getString(R.string.app_signature)))
