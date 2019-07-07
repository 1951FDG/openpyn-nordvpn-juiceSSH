/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.net;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ua.pp.msk.openvpnstatus.listeners.ByteCountManager.ByteCountListener;
import ua.pp.msk.openvpnstatus.listeners.LogManager.LogListener;
import ua.pp.msk.openvpnstatus.listeners.StateManager.StateListener;

/**
 * @author 1951FDG
 */

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

    void setUsernamePasswordHandler(@NotNull UsernamePasswordHandler handler);
    //endregion OpenVpnConnection
}
