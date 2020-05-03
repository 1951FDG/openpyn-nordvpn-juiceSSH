package io.github.getsixtyfour.openpyn

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.MenuItem
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.IAnalytic
import com.getsixtyfour.openvpnmgmt.android.Constants
import com.getsixtyfour.openvpnmgmt.android.Utils
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection
import com.google.firebase.analytics.FirebaseAnalytics
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPR.IGDPRCallback
import com.michaelflisar.gdprdialog.GDPRDefinitions
import com.michaelflisar.gdprdialog.GDPRSetup
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBar.OnActionClickListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.ktextension.juiceSSHInstall
import io.github.getsixtyfour.openpyn.map.util.createJson
import io.github.getsixtyfour.openpyn.map.util.stringifyJsonArray
import io.github.getsixtyfour.openpyn.settings.SettingsActivity
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import mu.KLoggable
import mu.KLogger
import mu.KotlinLogging
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.json.JSONArray
import timber.log.Timber
import tk.wasdennnoch.progresstoolbar.ProgressToolbar
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private val logger = KotlinLogging.logger {}

const val SNACK_BAR_JUICESSH: Int = 1
const val SNACK_BAR_PERMISSIONS: Int = 0

const val PRIORITY: String = "priority"
const val TAG: String = "tag"
const val MESSAGE: String = "message"

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

fun <T : Activity> getGDPR(activity: T, @StyleRes theme: Int): GDPRSetup {
    GDPR.getInstance().init(activity)
    return with(
        GDPRSetup(GDPRDefinitions.FABRIC_CRASHLYTICS, GDPRDefinitions.FIREBASE_CRASH, GDPRDefinitions.FIREBASE_ANALYTICS)
    ) {
        withCustomDialogTheme(theme)
        withForceSelection(true)
        withNoToolbarTheme(false)
        withShowPaidOrFreeInfoText(false)
    }
}

fun <T> showGDPRIfNecessary(activity: T, setup: GDPRSetup) where T : AppCompatActivity, T : IGDPRCallback {
    if (!AppConfig.GDPR) {
        return
    }

    if (isRunningTest()) {
        return
    }

    GDPR.getInstance().checkIfNeedsToBeShown(activity, setup)
}

fun <T : Activity> onAboutItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?) {
    SettingsActivity.startAboutFragment(activity)
}

fun <T : Activity> onGitHubItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?) {
    val uriString = "https://github.com/1951FDG/openpyn-nordvpn-juiceSSH"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    ContextCompat.startActivity(activity, intent, null)
}

fun <T : Activity> onRefreshItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem?) {
    //val drawable = item.icon as? Animatable
    //drawable?.start()
    val toolbar = activity.findViewById(R.id.toolbar) as? ProgressToolbar
    toolbar?.showProgress(true)

    activity.doAsync {
        var jsonArray: JSONArray? = null
        var json: String? = null
        var thrown = true
        if (NetworkInfo.getInstance().isOnline()) {
            jsonArray = createJson()
        }

        if (jsonArray != null) {
            json = when {
                BuildConfig.DEBUG -> stringifyJsonArray(jsonArray)
                else -> "$jsonArray"
            }
        }

        if (json != null) {
            try {
                val child = activity.resources.getResourceEntryName(R.raw.nordvpn) + ".json"
                val file = File(activity.getExternalFilesDir(null), child)
                file.writeText("$json\n")
                thrown = false
            } catch (e: NotFoundException) {
                logger.error(e) { "" }
            } catch (e: FileNotFoundException) {
                logger.error(e) { "" }
            } catch (e: IOException) {
                logger.error(e) { "" }
            }
        }

        activityUiThread {
            //drawable?.stop()
            toolbar?.hideProgress(true)

            if (!thrown) {
                /*MaterialDialog.Builder(it).apply {
                    title(R.string.title_dialog_warning)
                    content(R.string.warning_must_restart_app)
                    positiveText(android.R.string.ok)
                    show()
                }*/

                AlertDialog.Builder(it).apply {
                    setTitle(R.string.title_warning)
                    setMessage(R.string.warning_restart_app)
                    setPositiveButton(android.R.string.ok, null)
                    show()
                }
            }
        }

        onComplete {}
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
        else -> manager.getSnackProgressBar(storeId)?.let { manager.updateTo(it) }
    }
}

fun logException(throwable: Throwable) {
    if (Fabric.isInitialized()) {
        Crashlytics.logException(throwable)
    }
}

// TODO: inner class
@SuppressLint("DefaultLocale")
fun populateAboutConfig() {
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val buildType = BuildConfig.BUILD_TYPE.capitalize()
    val aboutConfig = AboutConfig.getInstance()
    // General info
    aboutConfig.appName = "openpyn-nordvpn-juiceSSH"
    aboutConfig.appIcon = R.mipmap.ic_launcher
    aboutConfig.version = "$buildType $versionName ($versionCode)"
    aboutConfig.author = "1951FDG"
    aboutConfig.companyHtmlPath = "https://github.com/" + aboutConfig.author
    aboutConfig.webHomePage = aboutConfig.companyHtmlPath + "/" + aboutConfig.appName
    aboutConfig.buildType = AboutConfig.BuildType.GOOGLE
    aboutConfig.packageName = BuildConfig.APPLICATION_ID
    // Custom analytics, dialog and share
    aboutConfig.analytics = object : IAnalytic, KLoggable {
        override fun logUiEvent(s: String, s1: String) {
            // Handle log events
        }

        override fun logException(e: Exception, b: Boolean) {
            // Handle exception events
            logger.error(e) { "" }
        }

        override val logger: KLogger
            get() = logger()
    }
    // Email
    aboutConfig.emailAddress = "support@1951fdg.com"
    aboutConfig.emailSubject = ""
    aboutConfig.emailBody = ""
    aboutConfig.emailBodyPrompt = ""
    // Share
    aboutConfig.shareMessage = ""
    aboutConfig.sharingTitle = "Share"
}

fun setDefaultPreferences(context: Context) {
    PreferenceManager.setDefaultValues(context, R.xml.pref_settings, false)
    PreferenceManager.setDefaultValues(context, R.xml.pref_api, true)
    PreferenceManager.setDefaultValues(context, R.xml.pref_openvpnmgmt, true)
    PreferenceManager.setDefaultValues(context, R.xml.pref_connect, true)
}

fun isRunningTest(): Boolean = try {
    Class.forName("androidx.test.espresso.Espresso")
    true
} catch (e: ClassNotFoundException) {
    false
}

fun <T : Activity> startVpnService(activity: T) {
    val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
    val openvpnmgmt = preferences.getBoolean(activity.getString(R.string.pref_openvpnmgmt_key), false)
    if (openvpnmgmt) {
        val handler = VpnAuthenticationHandler(activity)
        val host = VpnAuthenticationHandler.getHost(activity)
        val port = VpnAuthenticationHandler.getPort(activity)
        val password = VpnAuthenticationHandler.getPassword(activity)
        val shouldPostByteCount = VpnAuthenticationHandler.shouldPostByteCount(activity)
        val shouldPostStateChange = VpnAuthenticationHandler.shouldPostStateChange(activity)
        val bundle = Bundle().apply {
            putBoolean(Constants.EXTRA_POST_BYTE_COUNT_NOTIFICATION, shouldPostByteCount)
            putBoolean(Constants.EXTRA_POST_STATE_NOTIFICATION, shouldPostStateChange)
            putString(Constants.EXTRA_HOST, host)
            putInt(Constants.EXTRA_PORT, port)
            putCharArray(Constants.EXTRA_PASSWORD, password)
        }
        // TODO: do this elsewhere e.g. in Service, add missing intent extras and create handler in Service
        val connection = ManagementConnection.getInstance()
        connection.setUsernamePasswordHandler(handler)

        Utils.doStartService(activity, bundle)
    }
}

fun <T : Context> initCrashlytics(context: T) {
    val debug = BuildConfig.DEBUG
    if (debug) return
    val core = CrashlyticsCore.Builder().disabled(debug).build()
    Fabric.with(context, Crashlytics.Builder().core(core).build())
    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
    Timber.plant(CrashReportingTree()) // A tree which logs important information for crash reporting
}

fun initStrictMode() {
    if (!AppConfig.STRICT_MODE) {
        return
    }

    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
    )

    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
    )
}

@SuppressLint("PrivateApi")
fun <T : Context> isEmulator(context: T): Boolean {
    // TODO: add connectivity check for 10.0.2.2 on debug machine
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
        context.classLoader.loadClass("android.os.SystemProperties").let {
            if ((it.getMethod("get", String::class.java).invoke(it, "ro.kernel.qemu") as String) == "1") {
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

    PreferenceManager.getDefaultSharedPreferences(context).edit().let {
        // Android Emulator - Special alias to your host loopback interface (i.e., 127.0.0.1 on your development machine)
        it.putString(context.getString(R.string.pref_openvpnmgmt_host_key), "10.0.2.2")
        // The default port for Telnet client connections is 23
        it.putString(context.getString(R.string.pref_openvpnmgmt_port_key), "23")
        it.apply()
    }
}

private class CrashReportingTree : Timber.DebugTree() {
    override fun v(message: String?, vararg args: Any?) {
        // NOP
    }

    override fun v(t: Throwable?, message: String?, vararg args: Any?) {
        // NOP
    }

    override fun v(t: Throwable?) {
        // NOP
    }

    override fun d(message: String?, vararg args: Any?) {
        // NOP
    }

    override fun d(t: Throwable?, message: String?, vararg args: Any?) {
        // NOP
    }

    override fun d(t: Throwable?) {
        // NOP
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t == null) {
            return
        }

        if (!Fabric.isInitialized()) {
            return
        }

        Crashlytics.setInt(PRIORITY, priority)
        Crashlytics.setString(TAG, tag)
        Crashlytics.setString(MESSAGE, message)

        Crashlytics.logException(t)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority == android.util.Log.ERROR
    }
}
