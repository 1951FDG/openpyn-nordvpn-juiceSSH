package io.github.getsixtyfour.openpyn

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.IAnalytic
import com.google.android.gms.common.GooglePlayServicesUtil
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPR.IGDPRCallback
import com.michaelflisar.gdprdialog.GDPRSetup
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBar.OnActionClickListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import io.github.getsixtyfour.openpyn.utils.createJson
import io.github.getsixtyfour.openpyn.utils.stringifyJsonArray
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.json.JSONArray
import tk.wasdennnoch.progresstoolbar.ProgressToolbar
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"
const val SNACK_BAR_JUICESSH: Int = 1
const val SNACK_BAR_PERMISSIONS: Int = 0

fun <T : FragmentActivity> getCurrentNavigationFragment(activity: T): Fragment? {
    val navHostFragment = activity.supportFragmentManager.primaryNavigationFragment as? NavHostFragment
    val host = navHostFragment?.host
    if (host == null) {
        logException(IllegalStateException("Fragment $navHostFragment has not been attached yet."))
    }

    return when (host) {
        null -> null
        else -> navHostFragment.childFragmentManager.primaryNavigationFragment
    }
}

fun <T> showGDPRIfNecessary(activity: T, setup: GDPRSetup) where T : AppCompatActivity, T : IGDPRCallback {
    val debug = BuildConfig.DEBUG
    if (!debug) {
        GDPR.getInstance().checkIfNeedsToBeShown(activity, setup)
    }
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
        val debug = BuildConfig.DEBUG
        var jsonArray: JSONArray? = null
        var json: String? = null
        var thrown = true
        if (NetworkInfo.getInstance().isOnline()) {
            jsonArray = createJson()
        }

        if (jsonArray != null) {
            json = when {
                debug -> stringifyJsonArray(jsonArray)
                else -> jsonArray.toString()
            }
        }

        if (json != null) {
            try {
                val child = activity.resources.getResourceEntryName(R.raw.nordvpn) + ".json"
                val file = File(activity.getExternalFilesDir(null), child)
                file.writeText(json)
                thrown = false
            } catch (e: NotFoundException) {
                logException(e)
            } catch (e: FileNotFoundException) {
                logException(e)
            } catch (e: IOException) {
                logException(e)
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
                    setTitle(R.string.title_dialog_warning)
                    setMessage(R.string.warning_must_restart_app)
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

// todo inner class
fun <T : Activity> setSnackBarManager(activity: T, manager: SnackProgressBarManager) {
    fun snackProgressBar(type: Int, message: String, action: String, onActionClickListener: OnActionClickListener): SnackProgressBar {
        return SnackProgressBar(type, message).setAction(action, onActionClickListener)
    }

    val type = SnackProgressBar.TYPE_NORMAL
    val action = activity.getString(android.R.string.ok)

    manager.put(
        snackProgressBar(type, activity.getString(R.string.error_must_enable_permissions), action, object : OnActionClickListener {
            override fun onActionClick() {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(PERMISSION_READ, PERMISSION_OPEN_SESSIONS), MainActivity.PERMISSION_REQUEST_CODE
                )
            }
        }), SNACK_BAR_PERMISSIONS
    )

    manager.put(
        snackProgressBar(type, activity.getString(R.string.error_must_install_juicessh), action, object : OnActionClickListener {
            override fun onActionClick() {
                juiceSSHInstall(activity)
            }
        }), SNACK_BAR_JUICESSH
    )
}

fun <T : Activity> isJuiceSSHInstalled(activity: T): Boolean = try {
    activity.packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
    true
} catch (e: NameNotFoundException) {
    false
}

fun <T : Activity> juiceSSHInstall(activity: T) {
    fun openURI(uri: Uri, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        ContextCompat.startActivity(activity, intent, null)
    }

    try {
        activity.packageManager.getPackageInfo(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0)
        val uriBuilder =
            Uri.parse("https://play.google.com/store/apps/details").buildUpon().appendQueryParameter("id", JUICE_SSH_PACKAGE_NAME)
                .appendQueryParameter("launch", "true")
        try {
            openURI(uriBuilder.build(), GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE)
        } catch (e: ActivityNotFoundException) {
            openURI(uriBuilder.build())
        }
    } catch (e: NameNotFoundException) {
        val s = "juicessh-2-1-4"
        val uriString = "https://www.apkmirror.com/apk/sonelli-ltd/juicessh-ssh-client/$s-release/$s-android-apk-download/download/"
        openURI(Uri.parse(uriString))
    }
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

//todo inner class
@SuppressLint("DefaultLocale")
fun populateAboutConfig() {
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

fun setDefaultPreferences(context: Context) {
    PreferenceManager.setDefaultValues(context, R.xml.pref_settings, false)
    PreferenceManager.setDefaultValues(context, R.xml.pref_api, true)
    PreferenceManager.setDefaultValues(context, R.xml.pref_connect, true)
}

fun isRunningTest(): Boolean = try {
    Class.forName("androidx.test.espresso.Espresso")
    true
} catch (e: ClassNotFoundException) {
    false
}
