package com.getsixtyfour.openvpnmgmt.android;

import androidx.annotation.NonNull;

import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

final class OpenVpnHandler implements UsernamePasswordHandler {

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
