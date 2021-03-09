package io.github.getsixtyfour.functions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.core.content.ContextCompat
import io.github.getsixtyfour.ktextension.JUICE_SSH_PACKAGE_NAME
import io.github.getsixtyfour.openpyn.R

fun <T : Activity> showRefreshAlertDialog(activity: T) {
    Builder(activity).apply {
        setTitle(R.string.title_warning)
        setMessage(R.string.warning_restart_app)
        setPositiveButton(android.R.string.ok, null)
    }.show()
}

fun <T : Activity> showJuiceAlertDialog(activity: T) {
    Builder(activity).apply {
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
    Builder(activity).apply {
        setTitle(R.string.title_warning)
        setMessage(R.string.warning_install_openpyn)
        setPositiveButton(android.R.string.ok) { dialog, _ ->
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/1951FDG/openpyn-nordvpn/tree/test")).let {
                ContextCompat.startActivity((dialog as AlertDialog).context, it, null)
            }
        }
    }.show()
}
