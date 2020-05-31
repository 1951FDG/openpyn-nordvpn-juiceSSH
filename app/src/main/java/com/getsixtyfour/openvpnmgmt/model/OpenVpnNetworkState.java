package com.getsixtyfour.openvpnmgmt.model;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

public final class OpenVpnNetworkState {

    /**
     * time in UTC seconds
     */
    private final String mTime;

    /**
     * state name
     */
    private final String mName;

    /**
     * optional descriptive string
     */
    private final String mDescription;

    /**
     * optional TUN/TAP local IPv4 address
     */
    private final String mLocalAddress;

    /**
     * optional address of remote server
     */
    private final String mRemoteAddress;

    /**
     * optional port of remote server
     */
    private final String mRemotePort;

    public OpenVpnNetworkState(@NotNull String unixTime, @NotNull String name, @NotNull String description,
                               @NotNull String localAddress, @NotNull String remoteAddress, @NotNull String remotePort) {
        mTime = unixTime;
        mName = name;
        mDescription = description;
        mLocalAddress = localAddress;
        mRemoteAddress = remoteAddress;
        mRemotePort = remotePort;
    }

    public @NotNull String getTime() {
        return mTime;
    }

    public @NotNull String getName() {
        return mName;
    }

    public @NotNull String getDescription() {
        return mDescription;
    }

    public @NotNull String getLocalAddress() {
        return mLocalAddress;
    }

    public @NotNull String getRemoteAddress() {
        return mRemoteAddress;
    }

    public @NotNull String getRemotePort() {
        return mRemotePort;
    }

    public @NotNull Long getMillis() {
        return TimeUnit.MILLISECONDS.convert(Long.parseLong(mTime), TimeUnit.SECONDS);
    }
}
