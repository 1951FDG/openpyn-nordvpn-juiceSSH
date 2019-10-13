package com.getsixtyfour.openvpnmgmt.android.core;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.getsixtyfour.openvpnmgmt.android.Constants;
import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

/**
 * @author 1951FDG
 */

public final class VPNAuthenticationHandler implements UsernamePasswordHandler {

    private final Context mContext;

    @NonNull
    public static String getHost(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(Constants.PREF_MANAGEMENT_ADDRESS, Constants.DEF_HOST);
    }

    @NonNull
    public static String getPassword(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(Constants.PREF_MANAGEMENT_PASSWORD, Constants.DEF_PASSWORD);
    }

    @NonNull
    public static Integer getPort(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(Constants.PREF_MANAGEMENT_PORT, Constants.DEF_PORT);
    }

    @NonNull
    public static String getUserName(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(Constants.PREF_MANAGEMENT_USERNAME, Constants.DEF_USER_NAME);
    }

    public VPNAuthenticationHandler(@NonNull Context context) {
        mContext = context.getApplicationContext();
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
