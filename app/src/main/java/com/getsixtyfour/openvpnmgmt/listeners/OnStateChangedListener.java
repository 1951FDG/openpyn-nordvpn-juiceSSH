package com.getsixtyfour.openvpnmgmt.listeners;

import com.getsixtyfour.openvpnmgmt.model.OpenVpnNetworkState;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

@FunctionalInterface
public interface OnStateChangedListener {

    void onStateChanged(@NotNull OpenVpnNetworkState state);
}
