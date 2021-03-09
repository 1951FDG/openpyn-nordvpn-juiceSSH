package io.github.getsixtyfour.openpyn.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.getsixtyfour.openvpnmgmt.android.Constants
import com.getsixtyfour.openvpnmgmt.android.OpenVpnService
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.security.SecurityCypher

class ManagementService private constructor() {

    companion object {

        fun <T : Activity> start(activity: T) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
            val openvpnmgmt = preferences.getBoolean(activity.getString(R.string.pref_openvpnmgmt_key), false)
            if (openvpnmgmt) {
                val host = getHost(activity)
                val port = getPort(activity)
                val password = getPassword(activity)
                val userName = getUserName(activity)
                val userPass = getUserPass(activity)
                val shouldPostByteCount = shouldPostByteCount(activity)
                val shouldPostStateChange = shouldPostStateChange(activity)
                val bundle = Bundle().apply {
                    putBoolean(Constants.EXTRA_POST_BYTE_COUNT_NOTIFICATION, shouldPostByteCount)
                    putBoolean(Constants.EXTRA_POST_STATE_NOTIFICATION, shouldPostStateChange)
                    putString(Constants.EXTRA_HOST, host)
                    putInt(Constants.EXTRA_PORT, port)
                    putCharArray(Constants.EXTRA_PASSWORD, password)
                    putString(Constants.EXTRA_VPN_USERNAME, userName)
                    putString(Constants.EXTRA_VPN_PASSWORD, userPass)
                }

                doStartService(activity, bundle)
            }
        }

        fun <T : Activity> stop(activity: T) {
            doStopService(activity)
        }

        private fun doStartService(context: Context, extras: Bundle?) {
            val intent = Intent(context, OpenVpnService::class.java)
            intent.replaceExtras(extras)
            ContextCompat.startForegroundService(context, intent)
        }

        private fun doStopService(context: Context) {
            val intent = Intent(context, OpenVpnService::class.java)
            context.stopService(intent)
        }

        private fun getHost(context: Context): String {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_host_key), context.getString(R.string.pref_openvpnmgmt_host_default)
            )!!
        }

        private fun getPort(context: Context): Int {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return runCatching {
                preferences.getString(
                    context.getString(R.string.pref_openvpnmgmt_port_key), context.getString(R.string.pref_openvpnmgmt_port_default)
                )!!
            }.fold(String::toInt) { context.getString(R.string.pref_openvpnmgmt_port_default).toInt() }
        }

        private fun getPassword(context: Context): CharArray? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val string = preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_password_key), context.getString(R.string.pref_openvpnmgmt_password_default)
            )
            return if (string.isNullOrBlank()) null else SecurityCypher.getInstance(context).decrypt(string)
        }

        private fun shouldPostByteCount(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean(context.getString(R.string.pref_openvpnmgmt_bandwidth_usage_key), true)
        }

        private fun shouldPostStateChange(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean(context.getString(R.string.pref_openvpnmgmt_state_changes_key), true)
        }

        private fun getUserName(context: Context): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val string = preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_username_key), context.getString(R.string.pref_openvpnmgmt_username_default)
            )
            return if (string.isNullOrBlank()) null else string
        }

        private fun getUserPass(context: Context): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val string = preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_userpass_key), context.getString(R.string.pref_openvpnmgmt_userpass_default)
            )
            return if (string.isNullOrBlank()) null else SecurityCypher.getInstance(context).decryptString(string)
        }
    }
}
