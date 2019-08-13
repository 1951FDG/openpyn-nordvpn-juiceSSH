package ua.pp.msk.openvpnstatus.net;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;

import ua.pp.msk.openvpnstatus.api.Status;
import ua.pp.msk.openvpnstatus.exceptions.OpenVpnParseException;

/**
 * @author 1951FDG
 */

//@WorkerThread
public interface ConnectionBackground extends Closeable, Runnable {

    void connect(@NotNull String host, @NotNull Integer port) throws IOException;

    @NotNull
    String executeCommand(@NotNull String command) throws IOException;

    @NotNull
    Status getOpenVPNStatus() throws OpenVpnParseException, IOException;

    @NotNull
    String getOpenVPNVersion() throws IOException;

    void stopOpenVPN() throws IOException;
}
