package com.getsixtyfour.openvpnmgmt.api;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Calendar;

import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public interface Route {

    // Virtual Address,Common Name,Real Address,Last Ref
    @Nullable
    String getCommonName();

    /**
     * As I understand it should return connection date
     *
     * @return date since client acquired the connection
     */
    @Nullable
    Calendar getLastRef();

    @Nullable
    InetSocketAddress getRealIpAddress();

    @Nullable
    InetAddress getVirtualIpAddress();
}
