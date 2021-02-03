package io.github.getsixtyfour.openpyn

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.eggheadgames.aboutbox.AboutConfig
import com.getsixtyfour.openvpnmgmt.android.Constants
import com.getsixtyfour.openvpnmgmt.android.Utils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBar.OnActionClickListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.getsixtyfour.ktextension.juiceSSHInstall
import io.github.getsixtyfour.openpyn.map.util.createJson
import io.github.getsixtyfour.openpyn.map.util.generateXML
import io.github.getsixtyfour.openpyn.map.util.stringifyJsonArray
import io.github.getsixtyfour.openpyn.map.util.writeJsonArray
import io.github.getsixtyfour.openpyn.settings.SettingsActivity
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
const val SNACK_BAR_JUICESSH: Int = 1
const val SNACK_BAR_PERMISSIONS: Int = 0

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

fun <T : Activity> onAboutItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?) {
    SettingsActivity.startAboutFragment(activity)
}

fun <T : Activity> CoroutineScope.onGenerateItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?): Job = launch {
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

fun <T : Activity> onLogFileSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?) {
    val intent = Intent().apply { component = ComponentName(activity, "info.hannes.logcat.LogfileActivity") }
    ContextCompat.startActivity(activity, intent, null)
}

fun <T : Activity> CoroutineScope.onRefreshItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?): Job = launch {
    val toolbar = (activity.findViewById(R.id.toolbar) as? ProgressToolbar)?.apply { showProgress(true) }

    withContext(Dispatchers.IO) {
        runCatching {
            createJson()?.let(::stringifyJsonArray)?.let { writeJsonArray(activity, R.raw.nordvpn, it) }
        }
    }.onSuccess {
        toolbar?.hideProgress(true)
        AlertDialog.Builder(activity).apply {
            setTitle(R.string.title_warning)
            setMessage(R.string.warning_restart_app)
            setPositiveButton(android.R.string.ok, null)
        }.show()
    }.onFailure {
        toolbar?.hideProgress(true)
        logger.debug(it) { "" }
    }
}

fun <T : Activity> onSettingsItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?) {
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

// TODO: inner class
fun <T : Activity> setSnackBarManager(activity: T, manager: SnackProgressBarManager) {
    fun snackProgressBar(type: Int, message: String, action: String, onActionClickListener: OnActionClickListener): SnackProgressBar {
        return SnackProgressBar(type, message).setAction(action, onActionClickListener)
    }

    val type = SnackProgressBar.TYPE_NORMAL
    val action = activity.getString(android.R.string.ok)

    manager.put(
        snackProgressBar(type, activity.getString(R.string.error_juicessh_permissions), action, object : OnActionClickListener {
            override fun onActionClick() {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(PERMISSION_READ, PERMISSION_OPEN_SESSIONS), MainActivity.PERMISSION_REQUEST_CODE
                )
            }
        }), SNACK_BAR_PERMISSIONS
    )

    manager.put(
        snackProgressBar(type, activity.getString(R.string.error_juicessh_app), action, object : OnActionClickListener {
            override fun onActionClick() {
                activity.juiceSSHInstall()
            }
        }), SNACK_BAR_JUICESSH
    )
}

fun showSnackProgressBar(manager: SnackProgressBarManager, storeId: Int) {
    when (manager.getLastShown()) {
        null -> manager.show(storeId, SnackProgressBarManager.LENGTH_INDEFINITE)
        else -> manager.getSnackProgressBar(storeId)?.let(manager::updateTo)
    }
}

// TODO: inner class
@SuppressLint("DefaultLocale")
fun initAboutConfig(application: Application) {
    val config = AboutConfig.getInstance()
    // General info
    config.appName = application.getString(R.string.app_name)
    config.appIcon = R.mipmap.ic_launcher
    config.version = application.getString(R.string.app_version)
    config.buildType = AboutConfig.BuildType.GOOGLE
    config.packageName = application.getString(R.string.app_id)
    // Email
    config.emailAddress = application.getString(R.string.email_address)
    config.emailSubject = application.getString(R.string.empty)
    config.emailBody = application.getString(R.string.empty)
    config.emailBodyPrompt = application.getString(R.string.empty)
    // Share
    config.shareMessage = application.getString(R.string.empty)
    config.sharingTitle = application.getString(R.string.share)
}

fun initPreferences(application: Application) {
    PreferenceManager.setDefaultValues(application, R.xml.pref_settings, false)
    PreferenceManager.setDefaultValues(application, R.xml.pref_api, true)
    PreferenceManager.setDefaultValues(application, R.xml.pref_openvpnmgmt, true)
    PreferenceManager.setDefaultValues(application, R.xml.pref_connect, true)
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

fun initCrashlytics(application: Application) {
    if (BuildConfig.DEBUG) {
        return
    }
    val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    val telemetry = preferences.getBoolean("pref_telemetry", true)

    if (telemetry) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        Timber.plant(CrashReportingTree())
    }
}

fun initNetworkInfo(application: Application) {
    NetworkInfo.getInstance(application)
}
