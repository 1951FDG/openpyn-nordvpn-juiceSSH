package com.getsixtyfour.openvpnmgmt.net;

import org.jetbrains.annotations.NotNull;

public interface ConnectionListener {

    void onConnectError(@NotNull Throwable e);

    void onConnected();

    void onDisconnected();
}
