package io.github.getsixtyfour.functions

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.SettingsActivity
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import tk.wasdennnoch.progresstoolbar.ProgressToolbar

private val logger = KotlinLogging.logger {}

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
    if (!NetworkInfo.getInstance().isOnline()) {
        Toast.makeText(activity, activity.getString(R.string.info_no_connection), Toast.LENGTH_LONG).show()
        return@launch
    }
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
