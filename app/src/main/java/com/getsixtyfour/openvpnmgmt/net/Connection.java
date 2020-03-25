package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.OnRecordChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager.OnStateChangedListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    boolean addByteCountListener(@NotNull OnByteCountChangedListener listener);

    boolean addLogListener(@NotNull OnRecordChangedListener listener);

    boolean addStateListener(@NotNull OnStateChangedListener listener);

    boolean removeByteCountListener(@NotNull OnByteCountChangedListener listener);

    boolean removeLogListener(@NotNull OnRecordChangedListener listener);

    boolean removeStateListener(@NotNull OnStateChangedListener listener);

    void setConnectionListener(@Nullable ConnectionListener connectionListener);

    void setUsernamePasswordHandler(@Nullable UsernamePasswordHandler handler);
    //endregion OpenVpnConnection
}
