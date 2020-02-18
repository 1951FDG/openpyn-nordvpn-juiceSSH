package io.github.getsixtyfour.ktextension

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.common.GooglePlayServicesUtil

private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"

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

    try {
        packageManager.getPackageInfo(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0)
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
