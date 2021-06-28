package com.getsixtyfour.openvpnmgmt.android;

import androidx.annotation.NonNull;

import com.getsixtyfour.openvpnmgmt.core.AuthenticationHandler;

/**
 * @author 1951FDG
 */

final class OpenVpnHandler implements AuthenticationHandler {

    private final CharSequence mUser;

    private final CharSequence mPassword;

    OpenVpnHandler(@NonNull CharSequence user, @NonNull CharSequence password) {
        mUser = user;
        mPassword = password;
    }

    @Override
    public CharSequence getUser() {
        return mUser;
    }

    @Override
    public CharSequence getPassword() {
        return mPassword;
    }
}
