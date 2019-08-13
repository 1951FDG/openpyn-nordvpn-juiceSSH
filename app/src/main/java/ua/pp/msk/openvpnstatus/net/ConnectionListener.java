package ua.pp.msk.openvpnstatus.net;

import org.jetbrains.annotations.NotNull;

public interface ConnectionListener {

    void onConnectError(@NotNull Throwable e);

    void onConnected();

    void onDisconnected();
}
