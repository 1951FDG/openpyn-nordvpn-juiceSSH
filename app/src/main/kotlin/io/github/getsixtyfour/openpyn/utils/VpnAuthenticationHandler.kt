package io.github.getsixtyfour.openpyn.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.security.SecurityCypher

class VpnAuthenticationHandler(context: Context) : UsernamePasswordHandler {

    private val mContext: Context = context.applicationContext

    override fun getUser(): String? {
        return getUserName(mContext)
    }

    override fun getPassword(): String? {
        return getUserPass(mContext)
    }

    companion object {

        fun getHost(context: Context): String {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_host_key), context.getString(R.string.pref_openvpnmgmt_host_default)
            )!!
        }

        fun getPort(context: Context): Int {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return runCatching {
                preferences.getString(
                    context.getString(R.string.pref_openvpnmgmt_port_key), context.getString(R.string.pref_openvpnmgmt_port_default)
                )!!
            }.fold(String::toInt) { context.getString(R.string.pref_openvpnmgmt_port_default).toInt() }
        }

        fun getPassword(context: Context): CharArray? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val string = preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_password_key), context.getString(R.string.pref_openvpnmgmt_password_default)
            )
            return if (string.isNullOrBlank()) null else SecurityCypher.getInstance(context).decrypt(string)
        }

        fun shouldPostByteCount(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean(context.getString(R.string.pref_openvpnmgmt_bandwidth_usage_key), true)
        }

        fun shouldPostStateChange(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean(context.getString(R.string.pref_openvpnmgmt_state_changes_key), true)
        }

        internal fun getUserName(context: Context): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val string = preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_username_key), context.getString(R.string.pref_openvpnmgmt_username_default)
            )
            return if (string.isNullOrBlank()) null else string
        }

        internal fun getUserPass(context: Context): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val string = preferences.getString(
                context.getString(R.string.pref_openvpnmgmt_userpass_key), context.getString(R.string.pref_openvpnmgmt_userpass_default)
            )
            return if (string.isNullOrBlank()) null else SecurityCypher.getInstance(context).decryptString(string)
        }
    }
}
