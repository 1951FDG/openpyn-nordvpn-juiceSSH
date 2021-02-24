package io.github.getsixtyfour.ktextension

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.getsixtyfour.openpyn.R

const val JUICE_SSH_PACKAGE_NAME: String = "com.sonelli.juicessh"

fun Activity.isJuiceSSHInstalled(): Boolean = try {
    packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
    true
} catch (e: NameNotFoundException) {
    false
}

fun Activity.juiceSSHInstall() {
    fun openURI(uri: Uri, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        ContextCompat.startActivity(this, intent, null)
    }

    val installerPackageName = packageManager.getInstallerPackageName(packageName)
    if (installerPackageName == GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE) {
        val uriString = "https://play.google.com/store/apps/details"
        val uri = Uri.parse(uriString).buildUpon().apply {
            appendQueryParameter("id", JUICE_SSH_PACKAGE_NAME)
            appendQueryParameter("launch", "true")
        }.build()
        try {
            openURI(uri, installerPackageName)
        } catch (e: ActivityNotFoundException) {
            openURI(uri)
        }
    } else {
        val s = getString(R.string.juicessh_ssh_client)
        val uriString = "https://www.apkmirror.com/apk/sonelli-ltd/juicessh-ssh-client/$s-release/$s-android-apk-download/download/"
        val uri = Uri.parse(uriString)
        openURI(uri)
    }
}

fun Activity.startUpdate(appUpdateManager: AppUpdateManager, requestCode: Int) {
    appUpdateManager.appUpdateInfo.addOnSuccessListener {
        if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            appUpdateManager.startUpdateFlowForResult(it, AppUpdateType.IMMEDIATE, this, requestCode)
        }
    }
}

fun Activity.handleUpdate(appUpdateManager: AppUpdateManager, requestCode: Int) {
    appUpdateManager.appUpdateInfo.addOnSuccessListener {
        if (it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            appUpdateManager.startUpdateFlowForResult(it, AppUpdateType.IMMEDIATE, this, requestCode)
        }
    }
}
