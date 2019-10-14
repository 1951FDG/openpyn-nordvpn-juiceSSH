package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.gms.common.GooglePlayServicesUtil
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPR.IGDPRCallback
import com.michaelflisar.gdprdialog.GDPRSetup
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBar.OnActionClickListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.getsixtyfour.openpyn.utilities.NetworkInfo
import io.github.getsixtyfour.openpyn.utilities.createJson
import io.github.getsixtyfour.openpyn.utilities.logException
import io.github.getsixtyfour.openpyn.utilities.stringifyJsonArray
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
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

fun <T : AppCompatActivity> onAboutItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem) {
    SettingsActivity.startAboutFragment(activity)
}

fun <T : AppCompatActivity> onGitHubItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem) {
    val uriString = "https://github.com/1951FDG/openpyn-nordvpn-juiceSSH"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
    ContextCompat.startActivity(activity, intent, null)
}

fun <T : AppCompatActivity> onRefreshItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem) {
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

        uiThread {
            //drawable?.stop()
            toolbar?.hideProgress(true)

            if (!thrown) {
                MaterialDialog.Builder(it).title("Warning").content(R.string.warning_must_restart_app).positiveText(android.R.string.ok)
                    .show()
            }
        }

        onComplete {}
    }
}

fun <T : AppCompatActivity> onSettingsItemSelected(activity: T, @Suppress("UNUSED_PARAMETER") item: MenuItem) {
    SettingsActivity.startSettingsFragment(activity)
}

fun <T : AppCompatActivity> setProgressToolBar(activity: T, toolbar: ProgressToolbar) {
    toolbar.hideProgress()
    toolbar.isIndeterminate = true

    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.setDisplayShowTitleEnabled(false)
}

// todo inner class
fun <T : AppCompatActivity> setSnackBarManager(activity: T, manager: SnackProgressBarManager) {
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

fun isJuiceSSHInstalled(activity: Activity): Boolean = try {
    activity.packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
    true
} catch (e: NameNotFoundException) {
    false
}

fun juiceSSHInstall(activity: Activity) {
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
