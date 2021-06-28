package com.getsixtyfour.openvpnmgmt.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;

/**
 * @author 1951FDG
 */

public abstract class AbstractConnection implements Closeable {

    private @Nullable String mHost = null;

    private @Nullable BufferedReader mIn = null;

    private @Nullable BufferedWriter mOut = null;

    private @Nullable Integer mPort = null;

    private @Nullable Socket mSocket = null;

    private int mSocketConnectTimeout = 0;

    private int mSocketReadTimeout = 0;

    @Override
    public void close() throws IOException {
        if (mIn != null) {
            mIn.close();
        }
        if (mOut != null) {
            mOut.close();
        }
        if (mSocket != null) {
            mSocket.close();
        }
    }

    public void connect(@NotNull String host, @NotNull Integer port) throws IOException {
        connect(host, port, null);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    public void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) throws IOException {
        @NonNls Logger logger = getLogger();
        logger.info("Connecting to {}:{}", host, port);
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

    public void disconnect() throws IOException {
        @NonNls Logger logger = getLogger();
        logger.info("Disconnecting");
        close();
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

    protected abstract @NotNull Logger getLogger();

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
