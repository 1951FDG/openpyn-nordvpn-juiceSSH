package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.core.AuthenticationHandler;
import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.listeners.ConnectionStateListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnRecordChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnStateChangedListener;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */
// TODO: make android interface extension with worker thread annotation
public interface Connection {

    //region DeviceConnection (Background)
    void connect(@NotNull String host, @NotNull Integer port) throws IOException;

    void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) throws IOException;

    void disconnect();
    //endregion

    //region DeviceConnection
    boolean isConnected();
    //endregion

    //region ManagementConnection (Background)
    void sendCommand(@NotNull String command) throws IOException;

    @NotNull String sendCommand(@NotNull String command, int timeoutMillis) throws IOException;
    //endregion

    //region ManagementConnection
    boolean addOnByteCountChangedListener(@NotNull OnByteCountChangedListener listener);

    boolean removeOnByteCountChangedListener(@NotNull OnByteCountChangedListener listener);

    void clearOnByteCountChangedListeners();

    boolean addOnRecordChangedListener(@NotNull OnRecordChangedListener listener);

    boolean removeOnRecordChangedListener(@NotNull OnRecordChangedListener listener);

    void clearOnRecordChangedListeners();

    boolean addOnStateChangedListener(@NotNull OnStateChangedListener listener);

    boolean removeOnStateChangedListener(@NotNull OnStateChangedListener listener);

    void clearOnStateChangedListeners();
    //endregion

    //region DeviceConnection (Getter and Setter)
    @Nullable String getHost();

    @Nullable Integer getPort();

    int getSocketConnectTimeout();

    void setSocketConnectTimeout(int timeout);

    int getSocketReadTimeout();

    void setSocketReadTimeout(int timeout);
    //endregion

    //region ManagementConnection (Getter and Setter)
    void setAuthenticationHandler(@Nullable AuthenticationHandler handler);

    void setConnectionStateListener(@Nullable ConnectionStateListener listener);

    @NotNull ConnectionStatus getStatus();
    //endregion
    // TODO: mark all the methods to be called on background thread using @WorkerThread
}
