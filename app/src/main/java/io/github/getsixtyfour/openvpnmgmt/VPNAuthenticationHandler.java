package io.github.getsixtyfour.openvpnmgmt;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

import io.github.getsixtyfour.openpyn.R;
import io.github.getsixtyfour.security.SecurityManager;

/**
 * @author 1951FDG
 */

public final class VPNAuthenticationHandler implements UsernamePasswordHandler {

    private final Context mContext;

    public VPNAuthenticationHandler(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @NonNull
    public static String getHost(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_openvpnmgmt_host_key),
                context.getString(R.string.pref_openvpnmgmt_host_default));
    }

    @NonNull
    public static String getPassword(@NonNull Context context) {
        SecurityManager securityManager = SecurityManager.getInstance(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return securityManager.decryptString(preferences.getString(context.getString(R.string.pref_openvpnmgmt_password_key),
                context.getString(R.string.pref_openvpnmgmt_password_default)));
    }

    public static int getPort(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(preferences.getString(context.getString(R.string.pref_openvpnmgmt_port_key),
                context.getString(R.string.pref_openvpnmgmt_port_default)));
    }

    @NonNull
    public static String getUserName(@NonNull Context context) {
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
    @Override
    public String getUserPass() {
        return getPassword(mContext);
    }
}
