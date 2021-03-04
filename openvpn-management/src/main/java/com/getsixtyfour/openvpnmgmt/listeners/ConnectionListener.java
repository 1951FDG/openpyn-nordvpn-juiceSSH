package com.getsixtyfour.openvpnmgmt.listeners;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

public interface ConnectionListener {

    void onConnectError(@NotNull Thread t, @NotNull Throwable e);

    void onConnected(@NotNull Thread t);

    void onDisconnected(@NotNull Thread t);
}
