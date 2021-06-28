package com.getsixtyfour.openvpnmgmt.listeners;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

public interface ConnectionStateListener {

    void onConnect(@NotNull Thread t);

    void onDisconnect(@NotNull Thread t);
}
