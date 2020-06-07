package io.github.getsixtyfour.ktextension

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import java.security.MessageDigest

val Context.apkSignatures: List<String>
    get() = currentSignatures
val Context.currentSignatures: List<String>
    get() {
        return signatures().map {
            val messageDigest = MessageDigest.getInstance("SHA")
            messageDigest.update(it.toByteArray())
            Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP)
        }
    }

private fun Context.signatures(): Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    val signingInfo = packageInfo.signingInfo

    if (signingInfo.hasMultipleSigners()) {
        signingInfo.apkContentsSigners
    } else {
        signingInfo.signingCertificateHistory
    }
} else {
    @Suppress("DEPRECATION") @SuppressLint("PackageManagerGetSignatures") val packageInfo =
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    @Suppress("DEPRECATION") packageInfo.signatures
}

internal fun Context.verifySigningCertificate(appSignatures: List<String>): Boolean = appSignatures.containsAll(currentSignatures)

internal fun Context.verifyInstallerId(installerID: String): Boolean = packageManager.getInstallerPackageName(packageName) == installerID
