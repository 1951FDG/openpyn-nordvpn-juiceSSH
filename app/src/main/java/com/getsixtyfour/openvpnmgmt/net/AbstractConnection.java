package com.getsixtyfour.openvpnmgmt.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;

/**
 * @author 1951FDG
 */

abstract class AbstractConnection implements Closeable {

    private static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;

    private BufferedReader mBufferedReader;

    private BufferedWriter mBufferedWriter;

    private String mHost;

    private boolean mKeepAlive;

    private Integer mPort;

    private Socket mSocket;

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

    public void closeQuietly() {
        try {
            close();
        } catch (IOException ignored) {
        }
    }

    public String getHost() {
        return mHost;
    }

    public Integer getPort() {
        return mPort;
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        mKeepAlive = keepAlive;
    }

    protected void connect(String host, Integer port, char[] password) throws IOException {
        if (isConnected()) {
            throw new IOException("already connected");
        }
        if (host == null) {
            throw new IllegalArgumentException("hostname can't be null");
        }
        if (port == null) {
            throw new IllegalArgumentException("port can't be null");
        }
        mHost = host;
        mPort = port;
        connect(password);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private void connect(@Nullable char[] password) throws IOException {
        getLogger().info("Connecting to {}:{}", mHost, mPort); //NON-NLS
        mSocket = new Socket(mHost, mPort);
        mSocket.setKeepAlive(mKeepAlive);
        mSocket.setTcpNoDelay(true);
        InputStreamReader in = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);
        mBufferedReader = new BufferedReader(in, DEFAULT_CHAR_BUFFER_SIZE);
        OutputStreamWriter out = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
        mBufferedWriter = new BufferedWriter(out, DEFAULT_CHAR_BUFFER_SIZE);
        if (password != null) {
            mBufferedWriter.write(password);
            mBufferedWriter.newLine();
            mBufferedWriter.flush();
        }
    }

    protected void disconnect() {
        getLogger().info("Disconnecting"); //NON-NLS
        closeQuietly();
    }

    protected BufferedReader getBufferedReader() {
        return mBufferedReader;
    }

    protected BufferedWriter getBufferedWriter() {
        return mBufferedWriter;
    }

    protected abstract Logger getLogger();

    protected boolean isConnected() {
        return (mSocket != null) && mSocket.isConnected() && !mSocket.isClosed();
    }
}
