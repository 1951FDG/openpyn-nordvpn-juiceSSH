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

    boolean addByteCountListener(@NotNull ByteCountListener listener);

    boolean addLogListener(@NotNull LogListener listener);

    boolean addStateListener(@NotNull StateListener listener);

    boolean removeByteCountListener(@NotNull ByteCountListener listener);

    boolean removeLogListener(@NotNull LogListener listener);

    boolean removeStateListener(@NotNull StateListener listener);

    void setConnectionListener(@Nullable ConnectionListener connectionListener);

    void setUsernamePasswordHandler(@Nullable UsernamePasswordHandler handler);
    //endregion OpenVpnConnection
}
