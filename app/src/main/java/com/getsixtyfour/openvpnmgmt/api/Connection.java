package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.listeners.ConnectionListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnRecordChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnStateChangedListener;
import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

// TODO: make android interface extension with worker thread annotation
public interface Connection extends Runnable {

    //region ManagementConnection
    @Nullable String getHost();

    @Nullable Integer getPort();

    boolean isConnected();

    boolean isKeepAlive();

    void setKeepAlive(boolean keepAlive);
    //endregion

    //region ManagementConnection (Background)
    void connect(@NotNull String host, @NotNull Integer port);

    void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password);

    //TODO: not background necessary?
    void disconnect();
    //endregion

    //region OpenVpnConnection
    boolean isVpnActive();

    boolean addOnByteCountChangedListener(@NotNull OnByteCountChangedListener listener);

    boolean removeOnByteCountChangedListener(@NotNull OnByteCountChangedListener listener);

    void clearOnByteCountChangedListeners();

    boolean addOnRecordChangedListener(@NotNull OnRecordChangedListener listener);

    boolean removeOnRecordChangedListener(@NotNull OnRecordChangedListener listener);

    void clearOnRecordChangedListeners();

    boolean addOnStateChangedListener(@NotNull OnStateChangedListener listener);

    boolean removeOnStateChangedListener(@NotNull OnStateChangedListener listener);

    void clearOnStateChangedListeners();

    void setConnectionListener(@Nullable ConnectionListener connectionListener);

    void setUsernamePasswordHandler(@Nullable UsernamePasswordHandler handler);
    //endregion

    //region OpenVpnConnection (Background)
    @NotNull String executeCommand(@NotNull String command) throws IOException;

    void managementCommand(@NotNull String command) throws IOException;

    @NotNull String getManagementVersion() throws IOException;

    @NotNull Status getVpnStatus() throws IOException;

    @NotNull String getVpnVersion() throws IOException;
    //endregion
    // TODO: mark all the methods to be called on background thread using @WorkerThread
}
