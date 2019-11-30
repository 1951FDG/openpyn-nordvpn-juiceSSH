/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager.ByteCountListener;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.LogListener;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager.StateListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

@SuppressWarnings("unused")
public interface Connection extends ConnectionBackground {

    @Nullable
    String getHost();

    @Nullable
    Integer getPort();

    boolean isConnected();

    boolean isKeepAlive();

    void setKeepAlive(boolean keepAlive);

    //region ManagementConnection

    boolean isOpenVPNActive();

    void addByteCountListener(@NotNull ByteCountListener listener);

    void addLogListener(@NotNull LogListener listener);

    void addStateListener(@NotNull StateListener listener);

    void removeByteCountListener(@NotNull ByteCountListener listener);

    void removeLogListener(@NotNull LogListener listener);

    void removeStateListener(@NotNull StateListener listener);

    void setConnectionListener(@Nullable ConnectionListener connectionListener);

    void setUsernamePasswordHandler(@NotNull UsernamePasswordHandler handler); // todo notnull?
    //endregion OpenVpnConnection
}
