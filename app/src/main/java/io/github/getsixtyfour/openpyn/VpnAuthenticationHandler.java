package io.github.getsixtyfour.openpyn;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

import io.github.getsixtyfour.openpyn.security.SecurityManager;

public final class VpnAuthenticationHandler implements UsernamePasswordHandler {

    private final Context mContext;

    public VpnAuthenticationHandler(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @NonNull
    public static String getHost(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_openvpnmgmt_host_key),
                context.getString(R.string.pref_openvpnmgmt_host_default));
    }

    public static int getPort(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(preferences.getString(context.getString(R.string.pref_openvpnmgmt_port_key),
                context.getString(R.string.pref_openvpnmgmt_port_default)));
    }

    public static boolean shouldPostByteCount(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(context.getString(R.string.pref_openvpnmgmt_bandwidth_usage_key), true);
    }

    public static boolean shouldPostStateChange(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(context.getString(R.string.pref_openvpnmgmt_state_changes_key), true);
    }

    @NonNull
    private static String getUserName(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_openvpnmgmt_username_key),
                context.getString(R.string.pref_openvpnmgmt_username_default));
    }

    @NonNull
    @Override
    public String getUserName() {
        return getUserName(mContext);
    }

    @NonNull
    private static String getUserPass(@NonNull Context context) {
        SecurityManager securityManager = SecurityManager.getInstance(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return securityManager.decryptString(preferences.getString(context.getString(R.string.pref_openvpnmgmt_password_key),
                context.getString(R.string.pref_openvpnmgmt_password_default)));
    }

    @NonNull
    @Override
    public String getUserPass() {
        return getUserPass(mContext);
    }
}
