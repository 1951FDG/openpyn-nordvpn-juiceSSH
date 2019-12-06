package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author 1951FDG
 */

// @WorkerThread // todo all the methods below should be called on background thread
public interface ConnectionBackground extends Runnable {

    void connect(@NotNull String host, @NotNull Integer port) throws IOException;

    void disconnect();

    @NotNull
    String executeCommand(@NotNull String command) throws IOException;

    @NotNull
    Status getOpenVPNStatus() throws OpenVpnParseException, IOException;

    @NotNull
    String getOpenVPNVersion() throws IOException;

    void stopOpenVPN() throws IOException;
}
