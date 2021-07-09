package com.getsixtyfour.openvpnmgmt.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

public abstract class AbstractConnection {

    private @Nullable String mHost = null;

    private @Nullable BufferedReader mIn = null;

    private @Nullable BufferedWriter mOut = null;

    private @Nullable Integer mPort = null;

    private @Nullable Socket mSocket = null;

    private int mSocketConnectTimeout = 0;

    private int mSocketReadTimeout = 0;

    public void connect(@NotNull String host, @NotNull Integer port) throws IOException {
        connect(host, port, null);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) throws IOException {
        mHost = host;
        mPort = port;
        mSocket = new Socket();
        mSocket.setKeepAlive(false);
        mSocket.setSoTimeout(mSocketReadTimeout);
        mSocket.setTcpNoDelay(true);
        mSocket.connect(new InetSocketAddress(host, port), mSocketConnectTimeout);
        InputStreamReader reader = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);
        mIn = new BufferedReader(reader, 4096);
        OutputStreamWriter writer = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
        mOut = new BufferedWriter(writer, 4096);
        if ((password != null) && (password.length != 0)) {
            mOut.write(password);
            mOut.newLine();
            mOut.flush();
        }
    }

    public void disconnect() {
        try {
            if (mIn != null) {
                mIn.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (mOut != null) {
                mOut.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return (mSocket != null) && mSocket.isConnected() && !mSocket.isClosed();
    }

    public @Nullable String getHost() {
        return mHost;
    }

    public @Nullable Integer getPort() {
        return mPort;
    }

    public int getSocketConnectTimeout() {
        return mSocketConnectTimeout;
    }

    public void setSocketConnectTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        mSocketConnectTimeout = timeout;
    }

    public int getSocketReadTimeout() {
        return mSocketReadTimeout;
    }

    public void setSocketReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        mSocketReadTimeout = timeout;
    }

    protected @NotNull Socket getSocket() {
        return Objects.requireNonNull(mSocket);
    }

    protected @NotNull BufferedReader getSocketInputStream() {
        return Objects.requireNonNull(mIn);
    }

    protected @NotNull BufferedWriter getSocketOutputStream() {
        return Objects.requireNonNull(mOut);
    }
}
