package com.getsixtyfour.openvpnmgmt.api;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

public interface Client {

    @Nullable String getCommonName();

    @Nullable LocalDateTime getConnectedSince();

    @Nullable InetSocketAddress getIpAddress();

    long getReceivedBytes();

    long getSentBytes();
}
