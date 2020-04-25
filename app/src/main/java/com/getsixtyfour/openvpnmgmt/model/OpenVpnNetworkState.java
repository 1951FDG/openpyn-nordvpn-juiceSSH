package com.getsixtyfour.openvpnmgmt.model;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

public class OpenVpnNetworkState {

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

    @NotNull
    public String getTime() {
        return mTime;
    }

    @NotNull
    public String getName() {
        return mName;
    }

    @NotNull
    public String getDescription() {
        return mDescription;
    }

    @NotNull
    public String getLocalAddress() {
        return mLocalAddress;
    }

    @NotNull
    public String getRemoteAddress() {
        return mRemoteAddress;
    }

    @NotNull
    public String getRemotePort() {
        return mRemotePort;
    }

    @NotNull
    public Long getMillis() {
        return TimeUnit.MILLISECONDS.convert(Long.parseLong(mTime), TimeUnit.SECONDS);
    }
}
