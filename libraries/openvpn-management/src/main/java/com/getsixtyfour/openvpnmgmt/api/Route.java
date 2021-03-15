package com.getsixtyfour.openvpnmgmt.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;

import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

public interface Route {

    @Nullable String getCommonName();

    /**
     * As I understand it should return connection date
     *
     * @return date since client acquired the connection
     */
    @Nullable LocalDateTime getLastRef();

    @Nullable InetSocketAddress getRealIpAddress();

    @Nullable InetAddress getVirtualIpAddress();
}
