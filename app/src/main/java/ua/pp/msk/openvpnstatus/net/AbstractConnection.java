/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.pp.msk.openvpnstatus.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author 1951FDG
 */

abstract class AbstractConnection implements Closeable {

    private static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnection.class);

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
        } catch (IOException e) {
            // ignore
            // todo remove line
            LOGGER.warn(e.toString());
        }
    }

    private void connect() throws IOException {
        LOGGER.info("connecting to {}:{}", mHost, mPort);
        mSocket = new Socket(mHost, mPort);
        InputStreamReader in = new InputStreamReader(mSocket.getInputStream(), StandardCharsets.UTF_8);
        mBufferedReader = new BufferedReader(in, DEFAULT_CHAR_BUFFER_SIZE);
        OutputStreamWriter out = new OutputStreamWriter(mSocket.getOutputStream(), StandardCharsets.UTF_8);
        mBufferedWriter = new BufferedWriter(out, DEFAULT_CHAR_BUFFER_SIZE);
        mSocket.setKeepAlive(mKeepAlive);
        mSocket.setTcpNoDelay(true);
    }

    public void connect(String host, Integer port) throws IOException {
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
        connect();
    }

    public void disconnect() {
        LOGGER.info("disconnecting");
        closeQuietly();
    }

    public String getHost() {
        return mHost;
    }

    public Integer getPort() {
        return mPort;
    }

    public boolean isConnected() {
        return (mSocket != null) && mSocket.isConnected() && !mSocket.isClosed();
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        mKeepAlive = keepAlive;
    }

    protected BufferedReader getBufferedReader() {
        return mBufferedReader;
    }

    protected BufferedWriter getBufferedWriter() {
        return mBufferedWriter;
    }
}
