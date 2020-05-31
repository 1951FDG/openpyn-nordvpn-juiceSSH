package com.getsixtyfour.openvpnmgmt.api;

import java.net.InetSocketAddress;
import java.util.Calendar;

import org.jetbrains.annotations.Nullable;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public interface Client {

    @Nullable String getCommonName();

    @Nullable
    Calendar getConnectedSince();

    @Nullable InetSocketAddress getIpAddress();

    long getReceivedBytes();

    long getSentBytes();
}
