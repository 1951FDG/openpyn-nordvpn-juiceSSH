package com.getsixtyfour.openvpnmgmt.net;

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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;

/**
 * @author 1951FDG
 */

abstract class AbstractConnection implements Closeable {

    private static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;

    private @Nullable BufferedReader mBufferedReader = null;

    private @Nullable BufferedWriter mBufferedWriter = null;

    private @Nullable String mHost = null;

    private boolean mKeepAlive;

    private @Nullable Integer mPort = null;

    private @Nullable Socket mSocket = null;

    @Override
    public void close() throws IOException {
        if (mBufferedReader != null) {
            mBufferedReader.close();
        }
        if (mBufferedWriter != null) {
            mBufferedWriter.flush();
            mBufferedWriter.close();
        }
        if (mSocket != null) {
            mSocket.close();
        }
    }

    public @Nullable String getHost() {
        return mHost;
    }

    public @Nullable Integer getPort() {
        return mPort;
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        mKeepAlive = keepAlive;
    }

    protected void connect(@NotNull String host, @NotNull Integer port) throws IOException {
        connect(host, port, null);
    }

    protected void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) throws IOException {
        Logger logger = getLogger();
        logger.info("Connecting to {}:{}", host, port); //NON-NLS
        mHost = host;
        mPort = port;
        mSocket = new Socket();
        mSocket.connect(new InetSocketAddress(host, port), 1000);
        mSocket.setKeepAlive(mKeepAlive);
        mSocket.setTcpNoDelay(true);
        InputStreamReader in = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);
        mBufferedReader = new BufferedReader(in, DEFAULT_CHAR_BUFFER_SIZE);
        OutputStreamWriter out = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
        mBufferedWriter = new BufferedWriter(out, DEFAULT_CHAR_BUFFER_SIZE);
        if ((password != null) && (password.length != 0)) {
            mBufferedWriter.write(password);
            mBufferedWriter.newLine();
            mBufferedWriter.flush();
        }
    }

    protected void disconnect() throws IOException {
        Logger logger = getLogger();
        logger.info("Disconnecting"); //NON-NLS
        close();
    }

    protected @NotNull BufferedReader getBufferedReader() {
        return Objects.requireNonNull(mBufferedReader);
    }

    protected @NotNull BufferedWriter getBufferedWriter() {
        return Objects.requireNonNull(mBufferedWriter);
    }

    protected abstract @NotNull Logger getLogger();

    protected boolean isConnected() {
        return (mSocket != null) && mSocket.isConnected() && !mSocket.isClosed();
    }
}
