package com.getsixtyfour.openvpnmgmt.listeners;

import com.getsixtyfour.openvpnmgmt.core.StateManager.OpenVpnNetworkState;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

@FunctionalInterface
public interface OnStateChangedListener {

    void onStateChanged(@NotNull OpenVpnNetworkState state);
}
