package com.getsixtyfour.openvpnmgmt.net;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

public interface ConnectionListener {

    void onConnectError(@NotNull Throwable e);

    void onConnected();

    void onDisconnected();
}
